package aodv;

public interface MessageSender {

    void sendMessage(int address, byte[] data);

}
