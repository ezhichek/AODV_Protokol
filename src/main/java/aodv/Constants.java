package aodv;

public interface Constants {

    long ACTIVE_ROUTE_TIMEOUT   = 3000;
//    ALLOWED_HELLO_LOSS       2
//    BLACKLIST_TIMEOUT        RREQ_RETRIES * NET_TRAVERSAL_TIME
//    DELETE_PERIOD            see note below
//    HELLO_INTERVAL           1,000 Milliseconds
//    LOCAL_ADD_TTL            2
//    MAX_REPAIR_TTL           0.3 * NET_DIAMETER
//    MIN_REPAIR_TTL           see note below
    long MY_ROUTE_TIMEOUT       = 2 * ACTIVE_ROUTE_TIMEOUT;
    int NET_DIAMETER            = 35;
    long NODE_TRAVERSAL_TIME    = 40;
    long NET_TRAVERSAL_TIME     = 2 * NODE_TRAVERSAL_TIME * NET_DIAMETER;
    long NEXT_HOP_WAIT          = NODE_TRAVERSAL_TIME + 10;
    long PATH_DISCOVERY_TIME    = 2 * NET_TRAVERSAL_TIME;
//    RERR_RATELIMIT           10
    long TIMEOUT_BUFFER         = 2;
//    RREQ_RETRIES             2
//    RREQ_RATELIMIT           10
    int TTL_START               = 1;
    int TTL_INCREMENT           = 2;
    int TTL_THRESHOLD           = 7;
    int TTL_VALUE               = 64;
    long RING_TRAVERSAL_TIME    = 2 * NODE_TRAVERSAL_TIME * (TTL_VALUE + TIMEOUT_BUFFER);

}
