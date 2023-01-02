package aodv;

public interface MessageSender {

    void send(Message message, int destination);

}
