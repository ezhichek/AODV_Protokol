package aodv;

import java.util.ArrayList;
import java.util.List;

public class Route {

    private int destinationAddress;

    private int destinationSequenceNumber;

    private int hopCount;

    private int nextHop;

    private long lifetime;

    private boolean valid;

    private final List<Integer> precursors = new ArrayList<>();

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(int destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public int getDestinationSequenceNumber() {
        return destinationSequenceNumber;
    }

    public void setDestinationSequenceNumber(int destinationSequenceNumber) {
        this.destinationSequenceNumber = destinationSequenceNumber;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public int getNextHop() {
        return nextHop;
    }

    public void setNextHop(int nextHop) {
        this.nextHop = nextHop;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public void addPrecursor(int precursor) {
        precursors.add(precursor);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
