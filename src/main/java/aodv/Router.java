package aodv;

import java.util.HashMap;
import java.util.Map;

public class Router {

    private final int address = 65; // tbc

    private final Map<Integer, Integer> receivedRequests = new HashMap<>();

    private final Map<Integer, Route> routes = new HashMap<>();

    private int sequenceNumber;

    public void handleRouteRequest(RouteRequest request) {

        /*
        When a node receives a RREQ, it first creates or updates a route to
        the previous hop without a valid sequence number (see section 6.2)
        */
        createOrUpdateReverseRoute(request);

        /*
        then checks to determine whether it has received a RREQ with the same
        Originator IP Address and RREQ ID within at least the last
        PATH_DISCOVERY_TIME.  If such a RREQ has been received, the node
        silently discards the newly received RREQ.  The rest of this
        subsection describes actions taken for RREQs that are not discarded.
        */
        if (isEcho(request)) {
            return;
        }

        receivedRequests.put(request.getRequestId(), request.getOriginatorAddress());

        if (hasReachedDestination(request)) {

            generateRouteReplyFromDestination(request);

        } else if (hasValidRoute(request)) {

            generateRouteReplyFromIntermediateNode(request);

        } else {

            forwardRouteRequest(request);
        }
    }

    private void createOrUpdateReverseRoute(RouteRequest request) {

        /*
        When a node receives an AODV control packet from a neighbor, or
        creates or updates a route for a particular destination or subnet, it
        checks its route table for an entry for the destination.  In the
        event that there is no corresponding entry for that destination, an
        entry is created.  The sequence number is either determined from the
        information contained in the control packet, or else the valid
        sequence number field is set to false.  The route is only updated if
        the new sequence number is either

            (i)     higher than the destination sequence number in the route
                    table, or

            (ii)    the sequence numbers are equal, but the hop count (of the
                    new information) plus one, is smaller than the existing hop
                    count in the routing table, or

            (iii)   the sequence number is unknown.

        The Lifetime field of the routing table entry is either determined
        from the control packet, or it is initialized to
        ACTIVE_ROUTE_TIMEOUT.  This route may now be used to send any queued
        data packets and fulfills any outstanding route requests.
        */

        Route route = routes.get(request.getOriginatorAddress());

        if (route == null) {

            route = new Route();
            route.setDestinationAddress(request.getDestinationAddress());
            route.setDestinationSequenceNumber(request.getOriginatorSequence());
            route.setHopCount(1);
            route.setNextHop(request.getOriginatorAddress());
            route.setLifetime(Constants.ACTIVE_ROUTE_TIMEOUT);
            route.setValid(true);
            route.addPrecursor(request.getOriginatorAddress());

            routes.put(request.getDestinationAddress(), route);

        } else if (request.getOriginatorSequence() > route.getDestinationSequenceNumber()) {

            route.setDestinationSequenceNumber(request.getOriginatorSequence());
        }
    }

    private boolean isEcho(RouteRequest request) {
        final Integer origAddr = receivedRequests.get(request.getRequestId());
        return origAddr == request.getOriginatorAddress();
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

    private void generateRouteReplyFromDestination(RouteRequest request) {

        /*
        If the generating node is the destination itself, it MUST increment
        its own sequence number by one if the sequence number in the RREQ
        packet is equal to that incremented value.  Otherwise, the
        destination does not change its sequence number before generating the
        RREP message.
        */
        if (request.getDestinationSequence() == sequenceNumber + 1) {
            sequenceNumber++;
        }

        /*
        The destination node places its (perhaps newly
        incremented) sequence number into the Destination Sequence Number
        field of the RREP, and enters the value zero in the Hop Count field
        of the RREP.
        */
        final int destinationSequenceNumber = sequenceNumber;
        final int hopCount = 0;

        /*
        The destination node copies the value MY_ROUTE_TIMEOUT (see section
        10) into the Lifetime field of the RREP.  Each node MAY reconfigure
        its value for MY_ROUTE_TIMEOUT, within mild constraints (see section
        10).
        */
        final int lifetime = Constants.MY_ROUTE_TIMEOUT;

        /*
        When generating a RREP message, a node copies the Destination IP
        Address and the Originator Sequence Number from the RREQ message into
        the corresponding fields in the RREP message.
        */
        final RouteReply reply = new RouteReply(lifetime, request.getDestinationAddress(), destinationSequenceNumber, request.getOriginatorAddress(), hopCount);
    }

    private void generateRouteReplyFromIntermediateNode(RouteRequest request) {

        final Route forwardRoute = routes.get(request.getDestinationAddress());
        final Route reverseRoute = routes.get(request.getOriginatorAddress());

        /*
        If the node generating the RREP is not the destination node, but
        instead is an intermediate hop along the path from the originator to
        the destination, it copies its known sequence number for the
        destination into the Destination Sequence Number field in the RREP
        message.
        */
        final int destinationSequenceNumber = forwardRoute.getDestinationSequenceNumber();

        /*
        The intermediate node updates the forward route entry by placing the
        last hop node (from which it received the RREQ, as indicated by the
        source IP address field in the IP header) into the precursor list for
        the forward route entry -- i.e., the entry for the Destination IP
        Address.
        */
        forwardRoute.addPrecursor(request.getOriginatorAddress());

        /*
        The intermediate node also updates its route table entry
        for the node originating the RREQ by placing the next hop towards the
        destination in the precursor list for the reverse route entry --
        i.e., the entry for the Originator IP Address field of the RREQ
        message data.
        */
        reverseRoute.addPrecursor(forwardRoute.getNextHop());

        /*
        The intermediate node places its distance in hops from the
        destination (indicated by the hop count in the routing table) Count
        field in the RREP.
        */
        final int hopCount = forwardRoute.getHopCount();

        /*
        The Lifetime field of the RREP is calculated by
        subtracting the current time from the expiration time in its route
        table entry.
        */
        final int lifetime = forwardRoute.getLifetime() - 0; // System.currentTimeMillis();

        /*
        When generating a RREP message, a node copies the Destination IP
        Address and the Originator Sequence Number from the RREQ message into (typo? means originator ip address?)
        the corresponding fields in the RREP message.
        */
        final RouteReply reply = new RouteReply(lifetime, request.getDestinationAddress(), destinationSequenceNumber, request.getOriginatorAddress(), hopCount);
    }

    private void forwardRouteRequest(RouteRequest receivedRequest) {

        /*
        First, it first increments the hop count value in the RREQ by one, to
        account for the new hop through the intermediate node.
        */
        final int hopCount = receivedRequest.getHopCount() + 1;

        /*
        Then the node searches for a reverse route to the Originator IP Address (see
        section 6.2), using longest-prefix matching.
        */
        final Route reverseRoute = routes.get(receivedRequest.getOriginatorAddress());

        /*
        If need be, the route
        is created, or updated using the Originator Sequence Number from the
        RREQ in its routing table.  This reverse route will be needed if the
        node receives a RREP back to the node that originated the RREQ
        (identified by the Originator IP Address).
        */



        /*

        When the reverse route is
        created or updated, the following actions on the route are also
        carried out:

            1. the Originator Sequence Number from the RREQ is compared to the
               corresponding destination sequence number in the route table entry
               and copied if greater than the existing value there

            2. the valid sequence number field is set to true;

            3. the next hop in the routing table becomes the node from which the
               RREQ was received (it is obtained from the source IP address in
               the IP header and is often not equal to the Originator IP Address
               field in the RREQ message);

            4. the hop count is copied from the Hop Count in the RREQ message;

        Whenever a RREQ message is received, the Lifetime of the reverse
        route entry for the Originator IP address is set to be the maximum of
        (ExistingLifetime, MinimalLifetime), where

        MinimalLifetime =  (current time + 2*NET_TRAVERSAL_TIME - 2*HopCount*NODE_TRAVERSAL_TIME).

        The current node can use the reverse route to forward data packets in
        the same way as for any other route in the routing table.
        */

        final RouteRequest forwardedRequest = new RouteRequest(
                hopCount,
                receivedRequest.getRequestId(),
                receivedRequest.getDestinationAddress(),
                receivedRequest.getDestinationSequence(),
                receivedRequest.getOriginatorAddress(),
                receivedRequest.getOriginatorSequence());

        //routeRequest.setPrevHopAddress(this.nodeAddress);


    }





}
