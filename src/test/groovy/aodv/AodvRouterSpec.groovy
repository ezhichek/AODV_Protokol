package aodv

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static aodv.Utils.*

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
            routeToPrevHop.lifetime = ACTIVE_ROUTE_TIMEOUT

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME

        when:

            router.processRouteRequest(req, HOP_2)
            router.printRoutes()

        then:

            1 * callback.send(reply, HOP_2)
            getRoute(HOP_2) == routeToPrevHop
            getRoute(HOP_1) == reverseRoute

        when:

            router.processRouteRequest(req, HOP_2)

        then:

            0 * callback.send(_, _)

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
            putRoute(route)

            def routeToPrevHop = new Route(HOP_2)
            routeToPrevHop.destinationSequence = 0
            routeToPrevHop.destinationSequenceValid = false
            routeToPrevHop.hopCount = 1
            routeToPrevHop.nextHop = HOP_2
            routeToPrevHop.lifetime = ACTIVE_ROUTE_TIMEOUT

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME
            reverseRoute.addPrecursor(HOP_4)

            def forwardRoute = new Route(HOP_5)
            forwardRoute.destinationSequence = 10
            forwardRoute.destinationSequenceValid = true
            forwardRoute.hopCount = 2
            forwardRoute.nextHop = HOP_4
            forwardRoute.lifetime = clock.millis() + 4000
            forwardRoute.addPrecursor(HOP_2)

        when:

            router.processRouteRequest(req, HOP_2)

        then:

            1 * callback.send(reply, HOP_2)
            getRoute(HOP_2) == routeToPrevHop
            getRoute(HOP_1) == reverseRoute
            getRoute(HOP_5) == forwardRoute
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
                route.lifetime = active ? clock.millis() + 4000 : clock.millis() - 4000
                putRoute(route)
            }

            def routeToPrevHop = new Route(HOP_2)
            routeToPrevHop.destinationSequence = 0
            routeToPrevHop.destinationSequenceValid = false
            routeToPrevHop.hopCount = 1
            routeToPrevHop.nextHop = HOP_2
            routeToPrevHop.lifetime = ACTIVE_ROUTE_TIMEOUT

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME

        when:

            router.processRouteRequest(reqIn, HOP_2)

        then:

            1 * callback.send(reqOut, BROADCAST)
            getRoute(HOP_2) == routeToPrevHop
            getRoute(HOP_1) == reverseRoute

        where:

            active | destinationSequenceValid | destinationSequence | hasRoute
            false  | true                     | 10                  | true
            true   | false                    | 10                  | true
            true   | true                     | 9                   | true
            true   | true                     | 10                  | false
    }

    def "route reply is forwarded to next hop if current node is not the destination"() {

        given:

            def replyIn = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_1,              // originatorAddress
                    1                   // hopCount
            )

            def replyOut = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_1,              // originatorAddress
                    2                   // hopCount
            )

            def routeToPrevHop = new Route(HOP_4)
            routeToPrevHop.destinationSequence = 0
            routeToPrevHop.destinationSequenceValid = false
            routeToPrevHop.hopCount = 1
            routeToPrevHop.nextHop = HOP_4
            routeToPrevHop.lifetime = ACTIVE_ROUTE_TIMEOUT

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME
            putRoute(reverseRoute)

            def forwardRoute = new Route(HOP_5)
            forwardRoute.destinationSequence = 10
            forwardRoute.destinationSequenceValid = true
            forwardRoute.hopCount = 2
            forwardRoute.nextHop = HOP_4
            forwardRoute.lifetime = clock.millis() + 4000

        when:

            router.processRouteReply(replyIn, HOP_4)

        then:

            1 * callback.send(replyOut, HOP_2)
            getRoute(HOP_4) == routeToPrevHop
            getRoute(HOP_5) == forwardRoute
    }

    def "route reply is consumed if current node is the destination"() {

        given:

            def replyIn = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_1,              // originatorAddress
                    1                   // hopCount
            )

            def routeToPrevHop = new Route(HOP_4)
            routeToPrevHop.destinationSequence = 0
            routeToPrevHop.destinationSequenceValid = false
            routeToPrevHop.hopCount = 1
            routeToPrevHop.nextHop = HOP_4
            routeToPrevHop.lifetime = ACTIVE_ROUTE_TIMEOUT

            def reverseRoute = new Route(HOP_1)
            reverseRoute.destinationSequence = 2
            reverseRoute.destinationSequenceValid = true
            reverseRoute.hopCount = 2
            reverseRoute.nextHop = HOP_2
            reverseRoute.lifetime = clock.millis() + 2 * NET_TRAVERSAL_TIME - 2L * 2 * NODE_TRAVERSAL_TIME
            putRoute(reverseRoute)

            def forwardRoute = new Route(HOP_5)
            forwardRoute.destinationSequence = 10
            forwardRoute.destinationSequenceValid = true
            forwardRoute.hopCount = 2
            forwardRoute.nextHop = HOP_4
            forwardRoute.lifetime = clock.millis() + 4000

        when:

            router.processRouteReply(replyIn, HOP_4)

        then:

            1 * callback.send(_, _)
            getRoute(HOP_4) == routeToPrevHop
            getRoute(HOP_5) == forwardRoute
    }

    def "route reply is updating the forward route if the sequence number in the routing table is marked as invalid in route table entry"() {

        given:

            def replyIn = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_1,              // originatorAddress
                    1                   // hopCount
            )

            def forwardRoute = new Route(HOP_5)
            forwardRoute.destinationSequenceValid = true
            forwardRoute.destinationSequence = 7

            def expectedForwardRoute = new Route(HOP_5)
            expectedForwardRoute.destinationSequence = 10
            expectedForwardRoute.destinationSequenceValid = true
            expectedForwardRoute.hopCount = 2
            expectedForwardRoute.nextHop = HOP_4
            expectedForwardRoute.lifetime = clock.millis() + 4000

            def reverseRoute = new Route(HOP_1)
            reverseRoute.nextHop = HOP_2
            putRoute(reverseRoute)

        when:

            router.processRouteReply(replyIn, HOP_4)

        then:

            1 * callback.send(_, _)
            getRoute(HOP_5) == expectedForwardRoute
    }

    def "user data is forwarded if a route exists"() {

        given:

            def ud = new UserData(HOP_5, "test".getBytes())

            def forwardRoute = new Route(HOP_5)
            forwardRoute.destinationSequence = 10
            forwardRoute.destinationSequenceValid = true
            forwardRoute.hopCount = 2
            forwardRoute.nextHop = HOP_4
            forwardRoute.lifetime = clock.millis() + 10
            putRoute(forwardRoute)

            def updatedForwardRoute = new Route(HOP_5)
            updatedForwardRoute.destinationSequence = 10
            updatedForwardRoute.destinationSequenceValid = true
            updatedForwardRoute.hopCount = 2
            updatedForwardRoute.nextHop = HOP_4
            updatedForwardRoute.lifetime = clock.millis() + ACTIVE_ROUTE_TIMEOUT

        when:

            router.processUserData(ud, HOP_2)

        then:

            1 * callback.send(ud, HOP_4)
            getRoute(HOP_5) == updatedForwardRoute
    }

    def "route request is created and user data forwarding is retried if no route exists"() {

        given:

            def ud = new UserData(HOP_5, "test".getBytes())

            def updatedForwardRoute = new Route(HOP_5)
            updatedForwardRoute.destinationSequence = 10
            updatedForwardRoute.destinationSequenceValid = true
            updatedForwardRoute.hopCount = 2
            updatedForwardRoute.nextHop = HOP_4
            updatedForwardRoute.lifetime = clock.millis() + 4000

            def req = new RouteRequest(
                    0,                  // hopCount
                    1,                  // requestId
                    HOP_5,              // destinationAddress
                    0,                  // destinationSequence
                    true,               // destinationSequenceUnknown
                    HOP_3,              // originatorAddress
                    1                   // originatorSequence
            )

            def reply = new RouteReply(
                    4000,               // lifetime
                    HOP_5,              // destinationAddress
                    10,                 // destinationSequence
                    HOP_3,              // originatorAddress
                    1                   // hopCount
            )

        when:

            router.processUserData(ud, HOP_2)

        then:

            1 * callback.send(req, BROADCAST)

        when:

            router.processRouteReply(reply, HOP_4)
            sleep(3000)

        then:

            1 * callback.send(ud, HOP_4)
            getRoute(HOP_5) == updatedForwardRoute
    }

    def "error is raised after retrying to forward user data for 2 times"() {

        given:

            def ud = new UserData(HOP_5, "test".getBytes())

        when:

            router.processUserData(ud, HOP_2)

        then:

            1 * callback.send(new RouteRequest(0, 1, HOP_5, 0, true, HOP_3, 1), BROADCAST)

        when:

            sleep(3000)

        then:

            1 * callback.send(new RouteRequest(0, 2, HOP_5, 0, true, HOP_3, 2), BROADCAST)

        when:

            sleep(6000)

        then:

            1 * callback.send(new RouteRequest(0, 3, HOP_5, 0, true, HOP_3, 3), BROADCAST)

        when:

            sleep(12000)

        then:

            1 * callback.onError("Destination unreachable")
    }

    def putRoute(Route route) {
        router.routes.put(route.destinationAddress, route)
    }

    def getRoute(destinationAddress) {
        router.routes.get(destinationAddress)
    }

}
