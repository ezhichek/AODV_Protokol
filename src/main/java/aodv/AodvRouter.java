package aodv;

public interface AodvRouter {

    int getAddress();

    void setAddress(int address);

    void processRouteRequest(RouteRequest request, int prevHop);

    void processRouteReply(RouteReply reply, int prevHop);

    void processUserData(UserData data);

    void processUserData(UserData data, int prevHop);

    void printRoutes();
}
