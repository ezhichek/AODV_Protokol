package aodv

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static aodv.Utils.MY_ROUTE_TIMEOUT
import static aodv.Utils.NET_TRAVERSAL_TIME
import static aodv.Utils.NODE_TRAVERSAL_TIME

class AodvRouterSpec extends Specification {

    static int HOP_1 = 101
    static int HOP_2 = 102
    static int HOP_3 = 103
    static int HOP_4 = 104
    static int HOP_5 = 105

    static int BROADCAST = 0xFFFF

    RoutingCallback callback

    AodvRouterImpl router

    def clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

    def setup() {
        callback = Mock(RoutingCallback)
        router = new AodvRouterImpl(callback, clock)
        router.setAddress(HOP_3)

    }

    @Unroll
    def "route reply from destination node is created if destination has been reached"() {

        given:

            def req = new RouteRequest(
                    1,                  // hopCount
                    1,                  // requestId
                    HOP_3,              // destinationAddress
                    destinationSequence,// destinationSequence
                    false,              // destinationSequenceUnknown
                    HOP_1,              // originatorAddress
                    2                   // originatorSequence
            )

            def reply = new RouteReply(
                    MY_ROUTE_TIMEOUT,   // lifetime
                    HOP_3,              // destinationAddress
                    ownSequence,        // destinationSequence
                    HOP_1,              // originatorAddress
                    0                   // hopCount
            )

            def routeToPrevHop = new Route(HOP_2)
            routeToPrevHop.destinationSequence = 0
            routeToPrevHop.destinationSequenceValid = false
            routeToPrevHop.hopCount = 1
            routeToPrevHop.nextHop = HOP_2
            routeToPrevHop.lifetime = Utils.ACTIVE_ROUTE_TIMEOUT
            routeToPrevHop.active = false

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME
            reverseRoute.active = false

        when:

            router.processRouteRequest(req, HOP_2)

        then:

            1 * callback.send(reply, HOP_2)
            routeToPrevHop == getRoute(HOP_2)
            reverseRoute == getRoute(HOP_1)

        where:

            destinationSequence | ownSequence
            0                   | 0
            1                   | 1
            2                   | 0
            3                   | 0
    }

    def "route reply from intermediate node is created if an active route exists"() {

        given:

            def req = new RouteRequest(
                    1,                  // hopCount
                    1,                  // requestId
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    false,              // destinationSequenceUnknown
                    HOP_1,              // originatorAddress
                    2                   // originatorSequence
            )

            def reply = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_1,              // originatorAddress
                    2                   // hopCount
            )

            def route = new Route(HOP_5)
            route.destinationSequence = 10
            route.destinationSequenceValid = true
            route.hopCount = 2
            route.nextHop = HOP_4
            route.lifetime = clock.millis() + 4000
            route.active = true
            putRoute(route)

        when:

            router.processRouteRequest(req, HOP_2)

        then:

            1 * callback.send(reply, HOP_2)
    }

    def "route request is forwarded to broadcast address if no active route exists"() {

        given:

            def reqIn = new RouteRequest(
                    1,                  // hopCount
                    1,                  // requestId
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    false,              // destinationSequenceUnknown
                    HOP_1,              // originatorAddress
                    2                   // originatorSequence
            )

            def reqOut = new RouteRequest(
                    2,                  // hopCount
                    1,                  // requestId
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    false,              // destinationSequenceUnknown
                    HOP_1,              // originatorAddress
                    2                   // originatorSequence
            )

            if (hasRoute) {
                def route = new Route(HOP_5)
                route.destinationSequence = destinationSequence
                route.destinationSequenceValid = destinationSequenceValid
                route.hopCount = 2
                route.nextHop = HOP_4
                route.lifetime = clock.millis() + 4000
                route.active = active
                putRoute(route)
            }

        when:

            router.processRouteRequest(reqIn, HOP_2)

        then:

            1 * callback.send(reqOut, BROADCAST)

        where:

            active | destinationSequenceValid | destinationSequence | hasRoute
            false  | true                     | 10                  | true
            true   | false                    | 10                  | true
            true   | true                     | 9                   | true
            true   | true                     | 10                  | false
    }

    def putRoute(Route route) {
        router.routes.put(route.destinationAddress, route)
    }

    def getRoute(destinationAddress) {
        router.routes.get(destinationAddress)
    }

    def hasRoute(Route route) {
        def result = router.routes.get(route.destinationAddress)
        return result != null && result.equals(route)
    }

}