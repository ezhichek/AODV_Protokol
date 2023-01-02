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

        final Route reverseRoute = routes.computeIfAbsent(request.getOriginatorAddress(), k -> new Route());                    // Search for reverse route with matching Originator Address. If none exists, create a new one or update the current.
        reverseRoute.setDestinationAddress(request.getOriginatorAddress());                                                     // The originator's address becomes the new destination address
        reverseRoute.setDestinationSequence(Math.max(reverseRoute.getDestinationSequence(), request.getOriginatorSequence()));  // Set the Sequence Number to the max of (Destination Sequence Number of the route, Originator Sequence Number of the RREQ)
        reverseRoute.setValid(true);                                                                                            // The valid sequence number field is set to true;
        reverseRoute.setNextHop(previousHopAddress);                                                                            // the next hop in the routing table becomes the node from which the RREQ was received
        reverseRoute.setHopCount(request.getHopCount());                                                                        // Set the Hop Count from the RREQ's Hop Count.
        reverseRoute.setLifetime(Math.max(reverseRoute.getLifetime(), minLifetime(request.getHopCount())));                     // Whenever a RREQ message is received, the Lifetime of the reverse route entry for the Originator IP address is set to be the maximum of (ExistingLifetime, MinimalLifetime)

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

        // Create or update route to the previous hop without a valid Sequence Number.
        createRouteToPreviousHop(previousHopAddress);

        // Increment Hop Count in RREP.
        reply = reply.incrementHopCount();

        //         Search for a forward route to the Destination.
        // If none exists, create a new one or update the current (see Create or update Routes).
        // Send the RREP to the Originator Address using the reverse route.
        // Add the Next Hop node (target of our RREP) to the Precursor-list for the Destination Address.
        // Update the Lifetime for the reverse route to the max of (CurrentLifetime, CURRENT_TIMESTAMP + ACTIVE_ROUTE_TIMEOUT).
        // Add the Originator Adress of the RREP to the Precursor-list of the next hop towards the Destination Adress of the RREP.

    }

    private void createRouteToPreviousHop(int previousHopAddress) {

        Route route = routes.get(previousHopAddress);

        if (route == null) {

            route = new Route();
            route.setDestinationAddress(previousHopAddress);
            route.setDestinationSequence(0);
            route.setHopCount(1);
            route.setNextHop(previousHopAddress);
            route.setLifetime(ACTIVE_ROUTE_TIMEOUT);
            route.setValid(false);

            routes.put(previousHopAddress, route);
        }
    }

    private boolean hasValidRoute(RouteRequest request) {

        final Route route = routes.get(request.getDestinationAddress());

        /*
        it has an active route to the destination and
        the destination sequence number in the node's existing route table entry
        for the destination is valid and greater than or equal to
        the Destination Sequence Number of the RREQ (comparison using signed 32-bit arithmetic)
        */
        return route != null && route.isValid() && route.getDestinationSequence() >= request.getOriginatorSequence();
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
