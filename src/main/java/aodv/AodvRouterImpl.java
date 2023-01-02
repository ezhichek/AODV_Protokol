package aodv;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static aodv.Utils.*;

public class AodvRouterImpl implements AodvRouter {

    private final int address;

    private final MessageSender messageSender;

    private final Map<Integer, Integer> receivedRequests = new HashMap<>();

    private final Map<Integer, Route> routes = new HashMap<>();

    private int sequenceNumber = 0;

    public AodvRouterImpl(int address, MessageSender messageSender) {
        this.address = address;
        this.messageSender = messageSender;
    }

    @Override
    public void processRouteRequest(RouteRequest request, int previousHopAddress) {

        createRouteToPreviousHop(previousHopAddress);                                                                           // Create or update route to the previous hop without a valid Sequence Number.

        final Integer origAddr = receivedRequests.put(request.getRequestId(), request.getOriginatorAddress());
        if (origAddr != null && origAddr == request.getOriginatorAddress()) {                                                   // Discard, if we have seen this RREQ before (compare RREQ_ID and Originator Address).
            return;
        }

        request = request.incrementHopCount();                                                                                  // Increment the Hop Count on the RREQ.

        final Route reverseRoute = routes.computeIfAbsent(request.getOriginatorAddress(), Route::new);                          // Search for reverse route with matching Originator Address. If none exists, create a new one or update the current.
        reverseRoute.setDestinationSequence(Math.max(reverseRoute.getDestinationSequence(), request.getOriginatorSequence()));  // The Originator Sequence Number from the RREQ is compared to the corresponding destination sequence number in the route table entry and copied if greater than the existing value there
        reverseRoute.setDestinationSequenceValid(true);                                                                         // the valid sequence number field is set to true
        reverseRoute.setNextHop(previousHopAddress);                                                                            // The next hop in the routing table becomes the node from which the RREQ was received
        reverseRoute.setHopCount(request.getHopCount());                                                                        // The hop count is copied from the Hop Count in the RREQ message
        reverseRoute.setLifetime(Math.max(reverseRoute.getLifetime(), minLifetime(request.getHopCount())));                     // The Lifetime of the reverse route entry for the Originator IP address is set to be the maximum of (ExistingLifetime, MinimalLifetime)

        // If we are the Destination Address or do have a valid route, generate a RREP else forward the RREQ.
        if (request.getDestinationAddress() == address) {

            if (request.getDestinationSequence() == sequenceNumber + 1) {                                                       // If our own incremented sequence number (Sequence Number + 1) matches the Destination Sequence Number
                sequenceNumber++;                                                                                               // persist the incremented value, otherwise don't change it.
            }

            final RouteReply reply = new RouteReply(
                    MY_ROUTE_TIMEOUT,                                                                                           // Set Lifetime to the default MY_ROUTE_TIMEOUT.
                    request.getDestinationAddress(),
                    sequenceNumber,                                                                                             // Set the Destination Sequence Number in the RREP to own sequence number.
                    request.getOriginatorAddress(),
                    0                                                                                                           // Set Hop Count to 0.
            );

            sendRouteReply(reply, previousHopAddress);

        } else if (hasValidRoute(request)) {

            final Route forwardRoute = routes.get(request.getDestinationAddress());

            forwardRoute.addPrecursor(previousHopAddress);                                                                      // Add the RREQ's sender to the Precursor-list of the forward route.
            reverseRoute.addPrecursor(forwardRoute.getNextHop());                                                               // Add Next Hop from the forward route to the Precursor-list of the route to the Originator Adress of the RREQ (reverse route).

            final int lifetime = (int)(forwardRoute.getLifetime() - System.currentTimeMillis());                                // Set Lifetime in RREP to the difference between (forward Route Lifetime - Current Timestamp).

            final RouteReply reply = new RouteReply(
                    lifetime,
                    request.getDestinationAddress(),
                    forwardRoute.getDestinationSequence(),                                                                      // Set Destination Sequence to value of sequence from the forward route.
                    request.getOriginatorAddress(),
                    forwardRoute.getHopCount()                                                                                  // Set Hop Count in RREP to the value in the route to the Destination Adress of the RREP (forward route).
            );

            sendRouteReply(reply, previousHopAddress);

        } else {
            sendRouteRequest(request, BROADCAST_ADDRESS);
        }
    }

    @Override
    public void processRouteReply(RouteReply reply, int previousHopAddress) {

        createRouteToPreviousHop(previousHopAddress);                                                                           // Create or update route to the previous hop without a valid Sequence Number.

        reply = reply.incrementHopCount();                                                                                      // Increment Hop Count in RREP.

        final Route forwardRoute = routes.computeIfAbsent(reply.getDestinationAddress(), Route::new);                           // Search for reverse route with matching Originator Address. If none exists, create a new one or update the current
        if (!forwardRoute.isDestinationSequenceValid()                                                                          // The sequence number in the routing table is marked as invalid in route table entry
                || ((reply.getDestinationSequence()  > forwardRoute.getDestinationSequence())
                    && forwardRoute.isDestinationSequenceValid())                                                               // The Destination Sequence Number in the RREP is greater than the node's copy of the destination sequence number and the known value is valid
                || ((reply.getDestinationSequence() == forwardRoute.getDestinationSequence())
                    && !forwardRoute.isActive())                                                                                // The sequence numbers are the same, but the route is marked as inactive
                || ((reply.getDestinationSequence() == forwardRoute.getDestinationSequence())
                    && reply.getHopCount() < forwardRoute.getHopCount()))                                                       // The sequence numbers are the same, and the New Hop Count is smaller than the hop count in route table entry
        {
            forwardRoute.setActive(true);                                                                                       // The route is marked as active
            forwardRoute.setDestinationSequenceValid(true);                                                                     // The destination sequence number is marked as valid
            forwardRoute.setNextHop(previousHopAddress);                                                                        // The next hop in the route entry is assigned to be the node from which the RREP is received
            forwardRoute.setHopCount(reply.getHopCount());                                                                      // The hop count is set to the value of the New Hop Count
            forwardRoute.setLifetime(System.currentTimeMillis() + reply.getLifetime());                                         // The expiry time is set to the current time plus the value of the Lifetime in the RREP message
            forwardRoute.setDestinationSequence(reply.getDestinationSequence());                                                // The destination sequence number is the Destination Sequence Number in the RREP message
        }

        if (reply.getOriginatorAddress() != address) {                                                                          // If the current node is NOT the node indicated by the Originator IP Address in the RREP message
            final Route reverseRoute = routes.get(reply.getOriginatorAddress());                                                // Then the node consults its route table entry for the originating node to determine the next hop for the RREP
            sendRouteReply(reply, reverseRoute.getNextHop());                                                                   // And then forwards the RREP towards the originator using the information in that route table entry
        }
    }

    private void createRouteToPreviousHop(int previousHopAddress) {

        Route route = routes.get(previousHopAddress);

        if (route == null) {

            route = new Route(previousHopAddress);
            route.setDestinationSequence(0);
            route.setHopCount(1);
            route.setNextHop(previousHopAddress);
            route.setLifetime(ACTIVE_ROUTE_TIMEOUT);
            route.setDestinationSequenceValid(false);

            routes.put(previousHopAddress, route);
        }
    }

    private boolean hasValidRoute(RouteRequest request) {
        final Route forwardRoute = routes.get(request.getDestinationAddress());
        return forwardRoute != null && forwardRoute.isActive()                                                                  // An active route to the destination exists
                && forwardRoute.isDestinationSequenceValid()                                                                    // And the destination sequence in the route for the destination is valid
                && forwardRoute.getDestinationSequence() >= request.getOriginatorSequence();                                    // And the destination sequence in the route is greater than or equal to the destination sequence of the RREQ
    }

    private static long minLifetime(int hopCount) {
        return System.currentTimeMillis() + 2 * NET_TRAVERSAL_TIME - 2L * hopCount * NODE_TRAVERSAL_TIME;
    }

    private void sendRouteRequest(RouteRequest request, int address) {
        try {
            final byte[] data = request.serialize();
            final byte[] encodedData = Base64.getEncoder().encode(data);
            messageSender.sendMessage(address, encodedData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send route request", e);
        }
    }

    private void sendRouteReply(RouteReply reply, int address) {
        try {
            final byte[] data = reply.serialize();
            final byte[] encodedData = Base64.getEncoder().encode(data);
            messageSender.sendMessage(address, encodedData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send route reply", e);
        }
    }





}
