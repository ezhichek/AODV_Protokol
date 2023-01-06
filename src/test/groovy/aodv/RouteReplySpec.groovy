package aodv

import spock.lang.Specification
import spock.lang.Unroll

class RouteReplySpec extends Specification {

    @Unroll
    def "route reply serialization works"() {

        given:

            def reply1 = new RouteReply(
                    lifetime,
                    destinationAddress,
                    destinationSequence,
                    originatorAddress,
                    originatorSequence
            )

        when:

            def bytes = reply1.serialize()
            def reply2 = RouteReply.parse(bytes)

        then:

            reply1 == reply2

        where:

            lifetime | destinationAddress | destinationSequence | originatorAddress | originatorSequence
                   1 |                  1 |                   1 |                 1 |                  1
              262143 |              65535 |                 255 |             65535 |                255
                4000 |                105 |                  10 |                 5 |                  2
    }
}
