package aodv;

import java.io.IOException;

public interface Message {

    byte[] serialize() throws IOException;

}
