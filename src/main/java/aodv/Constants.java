package aodv;

public interface Constants {

    int ACTIVE_ROUTE_TIMEOUT   = 3000;
    int MY_ROUTE_TIMEOUT       = 2 * ACTIVE_ROUTE_TIMEOUT;
    int NET_DIAMETER           = 35;
    int NODE_TRAVERSAL_TIME    = 40;
    int NET_TRAVERSAL_TIME     = 2 * NODE_TRAVERSAL_TIME * NET_DIAMETER;
    int PATH_DISCOVERY_TIME    = 2 * NET_TRAVERSAL_TIME;
    int RREQ_RETRIES           = 2;

}
