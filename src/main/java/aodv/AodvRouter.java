package aodv;

import java.util.function.Consumer;

public interface AodvRouter {

    void processRouteRequest(RouteRequest request, int prevHop);

    void processRouteReply(RouteReply reply, int prevHop);

    void processUserData(UserData data, int prevHop, Consumer<String> errorHandler);

}
