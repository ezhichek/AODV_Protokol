package aodv;

public interface RoutingCallback {

    void send(Message message, int destination);

    void onError(String msg);

}
