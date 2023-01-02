package aodv;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static aodv.Utils.*;

public class AodvRouterImpl implements AodvRouter {

    private final int address;

    private final MessageSender messageSender;

    private final Map<Integer, Integer> receivedRequests = new HashMap<>();

    private final Map<Integer, Route> routes = new HashMap<>();

    private int sequenceNumber = 0;

    private int requestId = 0;

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AodvRouterImpl(int address, MessageSender messageSender) {
        this.address = address;
        this.messageSender = messageSender;
    }

    @Override
    public void processRouteRequest(RouteRequest request, int prevHop) {

        createRouteToPreviousHop(prevHop);                                                                                      // Create or update route to the previous hop without a valid Sequence Number.

        final Integer origAddr = receivedRequests.put(request.getRequestId(), request.getOriginatorAddress());
        if (origAddr != null && origAddr == request.getOriginatorAddress()) {                                                   // Discard, if we have seen this RREQ before (compare RREQ_ID and Originator Address).
            return;
        }

        request = request.incrementHopCount();                                                                                  // Increment the Hop Count on the RREQ.

        final Route reverseRoute = routes.computeIfAbsent(request.getOriginatorAddress(), Route::new);                          // Search for reverse route with matching Originator Address. If none exists, create a new one or update the current.
        reverseRoute.setDestinationSequence(Math.max(reverseRoute.getDestinationSequence(), request.getOriginatorSequence()));  // The Originator Sequence Number from the RREQ is compared to the corresponding destination sequence number in the route table entry and copied if greater than the existing value there
        reverseRoute.setDestinationSequenceValid(true);                                                                         // The valid sequence number field is set to true
        reverseRoute.setNextHop(prevHop);                                                                                       // The next hop in the routing table becomes the node from which the RREQ was received
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

            messageSender.send(reply, prevHop);

        } else if (hasValidRoute(request)) {

            final Route forwardRoute = routes.get(request.getDestinationAddress());

            forwardRoute.addPrecursor(prevHop);                                                                                 // Add the RREQ's sender to the Precursor-list of the forward route.
            reverseRoute.addPrecursor(forwardRoute.getNextHop());                                                               // Add Next Hop from the forward route to the Precursor-list of the route to the Originator Adress of the RREQ (reverse route).

            final int lifetime = (int)(forwardRoute.getLifetime() - System.currentTimeMillis());                                // Set Lifetime in RREP to the difference between (forward Route Lifetime - Current Timestamp).

            final RouteReply reply = new RouteReply(
                    lifetime,
                    request.getDestinationAddress(),
                    forwardRoute.getDestinationSequence(),                                                                      // Set Destination Sequence to value of sequence from the forward route.
                    request.getOriginatorAddress(),
                    forwardRoute.getHopCount()                                                                                  // Set Hop Count in RREP to the value in the route to the Destination Adress of the RREP (forward route).
            );

            messageSender.send(reply, prevHop);

        } else {

            messageSender.send(request, BROADCAST_ADDRESS);
        }
    }

    @Override
    public void processRouteReply(RouteReply reply, int prevHop) {

        createRouteToPreviousHop(prevHop);                                                                                      // Create or update route to the previous hop without a valid Sequence Number.

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
            forwardRoute.setNextHop(prevHop);                                                                                   // The next hop in the route entry is assigned to be the node from which the RREP is received
            forwardRoute.setHopCount(reply.getHopCount());                                                                      // The hop count is set to the value of the New Hop Count
            forwardRoute.setLifetime(System.currentTimeMillis() + reply.getLifetime());                                         // The expiry time is set to the current time plus the value of the Lifetime in the RREP message
            forwardRoute.setDestinationSequence(reply.getDestinationSequence());                                                // The destination sequence number is the Destination Sequence Number in the RREP message
        }

        if (reply.getOriginatorAddress() != address) {                                                                          // If the current node is NOT the node indicated by the Originator IP Address in the RREP message

            final Route reverseRoute = routes.get(reply.getOriginatorAddress());                                                // Then the node consults its route table entry for the originating node to determine the next hop for the RREP

            messageSender.send(reply, reverseRoute.getNextHop());                                                               // And then forwards the RREP towards the originator using the information in that route table entry
        }
    }

    @Override
    public void processUserData(UserData data, int prevHop, Consumer<String> errorHandler) {
        processUserData(data, prevHop, errorHandler, 0);
    }

    private void processUserData(UserData data, int prevHop, Consumer<String> errorHandler, int retries) {

        if (retries > RREQ_RETRIES) {
            errorHandler.accept("Destination unreachable");
            return;
        }

        final Route forwardRoute = routes.get(data.getDestinationAddress());
        if (forwardRoute == null || !forwardRoute.isActive()) {

            final RouteRequest request = new RouteRequest(
                    0,                                                                                                          // Set the Hop Count value to 0.
                    ++requestId,                                                                                                // Set the RREQ Request ID to increment of the last used Request ID.
                    data.getDestinationAddress(),
                    Optional.ofNullable(forwardRoute).map(Route::getDestinationSequence).orElse(0),                             // Set the RREQ Destination Sequence Number to the most up-to-date value.
                    forwardRoute == null,                                                                                       // Or set the Unknown Sequence Number-flag, if none is available.
                    address,
                    ++sequenceNumber                                                                                            // Set the RREQ Originator Sequence Number to the own sequence number, after it has been incremented for this step.
            );

            messageSender.send(request, BROADCAST_ADDRESS);                                                                     // Send route request

            final long delay = (long)Math.pow(2, retries) * NET_TRAVERSAL_TIME;
            scheduler.schedule(() -> processUserData(data, prevHop, errorHandler, retries + 1), delay, TimeUnit.MILLISECONDS);  // Buffer data
            return;
        }

        final long newLifetime = System.currentTimeMillis() + ACTIVE_ROUTE_TIMEOUT;

        forwardRoute.setLifetime(Math.max(forwardRoute.getLifetime(), newLifetime));

        final Route routeToPrevHop = routes.get(prevHop);
        if (routeToPrevHop != null) {
            routeToPrevHop.setLifetime(Math.max(routeToPrevHop.getLifetime(), newLifetime));
        }

        final Route routeToNextHop = routes.get(forwardRoute.getNextHop());
        if (routeToNextHop != null) {
            routeToNextHop.setLifetime(Math.max(routeToNextHop.getLifetime(), newLifetime));
        }

        messageSender.send(data, forwardRoute.getNextHop());
    }

    private void createRouteToPreviousHop(int previousHopAddress) {
        Route route = routes.get(previousHopAddress);
        if (route == null) {
            route = new Route(previousHopAddress);
            route.setActive(true);
            route.setDestinationSequence(0);
            route.setDestinationSequenceValid(false);
            route.setHopCount(1);
            route.setNextHop(previousHopAddress);
            route.setLifetime(ACTIVE_ROUTE_TIMEOUT);
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

}
