package aodv;

public class RouteReply {

    private static final int TYPE = 2;

    private final long lifetime;

    private final int destinationAddress;

    private final int destinationSequenceNumber;

    private final int originatorAddress;

    private final int hopCount;

    public RouteReply(long lifetime, int destinationAddress, int destinationSequenceNumber, int originatorAddress, int hopCount) {
        this.lifetime = lifetime;
        this.destinationAddress = destinationAddress;
        this.destinationSequenceNumber = destinationSequenceNumber;
        this.originatorAddress = originatorAddress;
        this.hopCount = hopCount;
    }

    public long getLifetime() {
        return lifetime;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationSequenceNumber() {
        return destinationSequenceNumber;
    }

    public int getOriginatorAddress() {
        return originatorAddress;
    }

    public int getHopCount() {
        return hopCount;
    }

    public byte[] serialize() {
        return null;
    }

    public static RouteReply parse(byte[] bytes) {
        return null;
    }

    public static boolean isRouteReply(byte[] bytes) {
        return false;
    }
}
