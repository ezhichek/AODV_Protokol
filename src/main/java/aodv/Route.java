package aodv;

import java.util.LinkedHashSet;
import java.util.Set;

public class Route {

    private final int destinationAddress;

    private int destinationSequence;

    private boolean validDestinationSequence;

    private int hopCount;

    private int nextHop;

    private long lifetime;

    private boolean active;

    private final Set<Integer> precursors = new LinkedHashSet<>();

    public Route(int destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationSequence() {
        return destinationSequence;
    }

    public void setDestinationSequence(int destinationSequence) {
        this.destinationSequence = destinationSequence;
    }

    public boolean isValidDestinationSequence() {
        return validDestinationSequence;
    }

    public void setValidDestinationSequence(boolean validDestinationSequence) {
        this.validDestinationSequence = validDestinationSequence;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addPrecursor(int precursor) {
        precursors.add(precursor);
    }
}
