package aodv;

import java.util.LinkedHashSet;
import java.util.Set;

public class Route {

    private int destinationAddress;

    private int destinationSequence;

    private int hopCount;

    private int nextHop;

    private long lifetime;

    private boolean valid;

    private final Set<Integer> precursors = new LinkedHashSet<>();

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(int destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public int getDestinationSequence() {
        return destinationSequence;
    }

    public void setDestinationSequence(int destinationSequence) {
        this.destinationSequence = destinationSequence;
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
