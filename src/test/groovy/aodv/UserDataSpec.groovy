package aodv

import spock.lang.Specification
import spock.lang.Unroll

class UserDataSpec extends Specification {

    @Unroll
    def "user data serialization works"() {

        given:

            def ud1 = new UserData(destinationAddress, data)

        when:

            def bytes = ud1.serialize()
            def ud2 = UserData.parse(bytes)

        then:

            ud1 == ud2

        where:

            destinationAddress | data
                             1 | "hallo".getBytes()
                         65535 | "hallo".getBytes()
                           105 | "hallo".getBytes()
                           105 | "".getBytes()
                           105 | new byte[0]
                           105 | null
    }
}
