package aodv;

public class RouteRequest {

    private static final int TYPE = 1;

    private final int hopCount;

    private final int requestId;

    private final int destinationAddress;

    private final int destinationSequenceNumber;

    private final int originatorAddress;

    private final int originatorSequenceNumber;

    public RouteRequest(int hopCount, int requestId, int destinationAddress, int destinationSequenceNumber, int originatorAddress, int originatorSequenceNumber) {
        this.hopCount = hopCount;
        this.requestId = requestId;
        this.destinationAddress = destinationAddress;
        this.destinationSequenceNumber = destinationSequenceNumber;
        this.originatorAddress = originatorAddress;
        this.originatorSequenceNumber = originatorSequenceNumber;
    }

    public int getHopCount() {
        return hopCount;
    }

    public int getRequestId() {
        return requestId;
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

    public int getOriginatorSequenceNumber() {
        return originatorSequenceNumber;
    }

    public byte[] serialize() {
        return null;
    }

    public static RouteRequest parse(byte[] bytes) {
        return null;
    }

    public static boolean isRouteRequest(byte[] bytes) {
        return false;
    }
}
