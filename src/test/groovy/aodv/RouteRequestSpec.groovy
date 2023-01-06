package aodv

import spock.lang.Specification
import spock.lang.Unroll

class RouteRequestSpec extends Specification {

    @Unroll
    def "route request serialization works"() {

        given:

            def req1 = new RouteRequest(
                    hopCount,
                    requestId,
                    destinationAddress,
                    destinationSequence,
                    destinationSequenceUnknown,
                    originatorAddress,
                    originatorSequence
            )

        when:

            def bytes = req1.serialize()
            def req2 = RouteRequest.parse(bytes)

        then:

            req1 == req2

        where:

            hopCount | requestId | destinationAddress | destinationSequence | destinationSequenceUnknown | originatorAddress | originatorSequence
                   1 |         1 |                  1 |                   1 |                      false |                 1 |                  1
                  63 |        63 |              65535 |                 255 |                      false |             65535 |                255
                   7 |        10 |                105 |                  10 |                      false |                 5 |                  2
                   7 |        10 |                105 |                  10 |                       true |                 5 |                  2
    }
}
