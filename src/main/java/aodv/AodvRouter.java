package aodv;

public interface AodvRouter {

    void setAddress(int address);

    void processRouteRequest(RouteRequest request, int prevHop);

    void processRouteReply(RouteReply reply, int prevHop);

    void processUserData(UserData data, int prevHop);

}
