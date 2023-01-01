package aodv;

import java.util.HashMap;
import java.util.Map;

public class Router {

    private final int address = 65; // tbc

    private final Map<Integer, Integer> receivedRequests = new HashMap<>();

    private final Map<Integer, Route> routes = new HashMap<>();

    private int sequenceNumber;

    public void processRouteRequest(RouteRequest request, int previousHopAddress) {

        // Create or update route to the previous hop without a valid Sequence Number.
        createRouteToPreviousHop(previousHopAddress);

        // Discard, if we have seen this RREQ before.
        if (isEcho(request)) {
            return;
        }

        receivedRequests.put(request.getRequestId(), request.getOriginatorAddress());

        // Increment the Hop Count on the RREQ.
        // -> we are doing this in forwardRouteRequest and createOrUpdateReverseRoute

        // Search for reverse route with matching Originator Address.
        // If none exists, create a new one or update the current.
        createOrUpdateReverseRoute(request, previousHopAddress);

        // If we are the Destination Address or do have a valid route, generate a RREP else forward the RREQ.
        if (hasReachedDestination(request)) {

            generateRouteReplyFromDestination(request.getOriginatorAddress(), request.getDestinationAddress(), request.getDestinationSequence());

        } else if (hasValidRoute(request)) {

            generateRouteReplyFromIntermediateNode(request.getOriginatorAddress(), request.getDestinationAddress(), previousHopAddress);

        } else {

            broadcastRouteRequest(request);
        }
    }

    private void createRouteToPreviousHop(int previousHopAddress) {

        Route route = routes.get(previousHopAddress);

        if (route == null) {

            route = new Route();
            route.setDestinationAddress(previousHopAddress);
            route.setDestinationSequenceNumber(0);
            route.setHopCount(1);
            route.setNextHop(previousHopAddress);
            route.setLifetime(Constants.ACTIVE_ROUTE_TIMEOUT);
            route.setValid(false);

            routes.put(previousHopAddress, route);
        }
    }

    private boolean isEcho(RouteRequest request) {
        // Check if we have seen the RREQ before (compare RREQ_ID and Originator Address).
        final Integer origAddr = receivedRequests.get(request.getRequestId());
        return origAddr == request.getOriginatorAddress();
    }

    private void createOrUpdateReverseRoute(RouteRequest request, int previousHopAddress) {

        // Increment the Hop Count on the RREQ.
        final int hopCount = request.getHopCount() + 1;

        final Route route = routes.computeIfAbsent(request.getOriginatorAddress(), k -> new Route());

        // The originator's address becomes the new destination address
        route.setDestinationAddress(request.getOriginatorAddress());

        // Set the Sequence Number to the max of (Destination Sequence Number of the route, Originator Sequence Number of the RREQ)
        route.setDestinationSequenceNumber(Math.max(route.getDestinationSequenceNumber(), request.getOriginatorSequence()));

        // The valid sequence number field is set to true;
        route.setValid(true);

        // the next hop in the routing table becomes the node from which the RREQ was received
        route.setNextHop(previousHopAddress);

        // Set the Hop Count from the RREQ's Hop Count + 1.
        route.setHopCount(hopCount);

        // Whenever a RREQ message is received, the Lifetime of the reverse route entry for the Originator IP address is set to be the maximum of
        // (ExistingLifetime, MinimalLifetime), where
        // MinimalLifetime = (current time + 2 * NET_TRAVERSAL_TIME - 2 * HopCount * NODE_TRAVERSAL_TIME).
        final long minLifetime = System.currentTimeMillis() + 2 * Constants.NET_TRAVERSAL_TIME - 2 * hopCount * Constants.NODE_TRAVERSAL_TIME;
        route.setLifetime(Math.max(route.getLifetime(), minLifetime));
    }


    private boolean hasReachedDestination(RouteRequest request) {
        return request.getDestinationAddress() == address;
    }

    private boolean hasValidRoute(RouteRequest request) {

        final Route route = routes.get(request.getDestinationAddress());

        /*
        it has an active route to the destination and
        the destination sequence number in the node's existing route table entry
        for the destination is valid and greater than or equal to
        the Destination Sequence Number of the RREQ (comparison using signed 32-bit arithmetic)
        */
        return route != null && route.isValid() && route.getDestinationSequenceNumber() >= request.getOriginatorSequence();
    }

    private void generateRouteReplyFromDestination(int originatorAddress, int destinationAddress, int destinationSequence) {

        // Set Hop Count to 0.
        final int hopCount = 0;

        // Set Lifetime to the default MY_ROUTE_TIMEOUT.
        final int lifetime = Constants.MY_ROUTE_TIMEOUT;

        // If our own incremented sequence number (Sequence Number + 1) matches the Destination Sequence Number,
        // persist the incremented value, otherwise don't change it.
        if (destinationSequence == sequenceNumber + 1) {
            sequenceNumber++;
        }

        // Set the Destination Sequence Number in the RREP to own sequence number.
        final int destinationSequenceNumber = sequenceNumber;

        final RouteReply reply = new RouteReply(lifetime, destinationAddress, destinationSequenceNumber, originatorAddress, hopCount);
    }

    private void generateRouteReplyFromIntermediateNode(int originatorAddress, int destinationAddress, int previousHopAddress) {

        final Route forwardRoute = routes.get(destinationAddress);
        final Route reverseRoute = routes.get(originatorAddress);

        // Set Destination Sequence Number to value of sequence number from the forward route.
        final int destinationSequenceNumber = forwardRoute.getDestinationSequenceNumber();

        // Add the RREQ's sender to the Precursor-list of the forward route.
        forwardRoute.addPrecursor(previousHopAddress);

        // Add Next Hop from the forward route to the Precursor-list of the route to the Originator Adress of the RREQ (reverse route).
        reverseRoute.addPrecursor(forwardRoute.getNextHop());

        // Set Hop Count in RREP to the value in the route to the Destination Adress of the RREP (forward route).
        final int hopCount = forwardRoute.getHopCount();

        // Set Lifetime in RREP to the difference between (forward Route Lifetime - Current Timestamp).
        final int lifetime = (int)(forwardRoute.getLifetime() - System.currentTimeMillis());

        final RouteReply reply = new RouteReply(lifetime, destinationAddress, destinationSequenceNumber, originatorAddress, hopCount);
    }

    private void broadcastRouteRequest(RouteRequest receivedRequest) {

        // Increment the Hop Count on the RREQ.
        final int hopCount = receivedRequest.getHopCount() + 1;

        final RouteRequest forwardedRequest = new RouteRequest(
                hopCount,
                receivedRequest.getRequestId(),
                receivedRequest.getDestinationAddress(),
                receivedRequest.getDestinationSequence(),
                receivedRequest.getOriginatorAddress(),
                receivedRequest.getOriginatorSequence());


    }





}
