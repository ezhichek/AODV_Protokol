package aodv;

public interface AodvRouter {

    void processRouteRequest(RouteRequest request, int previousHopAddress);

    void processRouteReply(RouteReply reply, int previousHopAddress);

}
