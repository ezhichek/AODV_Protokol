package aodv;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.LinkedHashSet;
import java.util.Set;

public class Route {

    private final int destinationAddress;

    private int destinationSequence;

    private boolean destinationSequenceValid;

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

    public boolean isDestinationSequenceValid() {
        return destinationSequenceValid;
    }

    public void setDestinationSequenceValid(boolean destinationSequenceValid) {
        this.destinationSequenceValid = destinationSequenceValid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Route route = (Route)o;
        return new EqualsBuilder()
                .append(destinationAddress, route.destinationAddress)
                .append(destinationSequence, route.destinationSequence)
                .append(destinationSequenceValid, route.destinationSequenceValid)
                .append(hopCount, route.hopCount)
                .append(nextHop, route.nextHop)
                .append(lifetime, route.lifetime)
                .append(active, route.active)
                .append(precursors, route.precursors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(destinationAddress)
                .append(destinationSequence)
                .append(destinationSequenceValid)
                .append(hopCount)
                .append(nextHop)
                .append(lifetime)
                .append(active)
                .append(precursors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("destinationAddress", destinationAddress)
                .append("destinationSequence", destinationSequence)
                .append("destinationSequenceValid", destinationSequenceValid)
                .append("hopCount", hopCount)
                .append("nextHop", nextHop)
                .append("lifetime", lifetime)
                .append("active", active)
                .append("precursors", precursors)
                .toString();
    }
}
