package aodv;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;

import static aodv.Utils.*;

public class RouteRequest implements Message {

    private static final int TYPE = 1;

    private final int hopCount;

    private final int requestId;

    private final int destinationAddress;

    private final int destinationSequence;

    private final boolean destinationSequenceUnknown;

    private final int originatorAddress;

    private final int originatorSequence;

    public RouteRequest(int hopCount, int requestId, int destinationAddress, int destinationSequence, boolean destinationSequenceUnknown, int originatorAddress, int originatorSequence) {
        this.hopCount = validate(hopCount, 0, MAX_6_BITS);
        this.requestId =  validate(requestId, 0, MAX_6_BITS);
        this.destinationAddress = validate(destinationAddress, 0, MAX_16_BITS);
        this.destinationSequence = validate(destinationSequence, 0, MAX_8_BITS);
        this.destinationSequenceUnknown = destinationSequenceUnknown;
        this.originatorAddress = validate(originatorAddress, 0, MAX_16_BITS);
        this.originatorSequence = validate(originatorSequence, 0, MAX_8_BITS);
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

    public int getDestinationSequence() {
        return destinationSequence;
    }

    public boolean isDestinationSequenceUnknown() {
        return destinationSequenceUnknown;
    }

    public int getOriginatorAddress() {
        return originatorAddress;
    }

    public int getOriginatorSequence() {
        return originatorSequence;
    }

    public RouteRequest incrementHopCount() {
        return new RouteRequest(hopCount + 1, requestId, destinationAddress, destinationSequence, destinationSequenceUnknown, originatorAddress, originatorSequence);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("hopCount", hopCount)
                .append("requestId", requestId)
                .append("destinationAddress", destinationAddress)
                .append("destinationSequence", destinationSequence)
                .append("destinationSequenceUnknown", destinationSequenceUnknown)
                .append("originatorAddress", originatorAddress)
                .append("originatorSequence", originatorSequence)
                .toString();
    }

    public byte[] serialize() throws IOException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final DataOutput output = new DataOutputStream(bos);

        int block1 = 0;
        // type - 6 bits
        block1 |= (TYPE & INT_MASK) << 18;
        // flags - 6 bits
        if (destinationSequenceUnknown) {
            block1 |= (1 & INT_MASK) << 17;
        }
        // hop count - 6 bits
        block1 |= (hopCount & INT_MASK) << 6;
        // request id - 6 bits
        block1 |= requestId & INT_MASK;
        output.write(toBytes(block1, 3));

        int block2 = 0;
        // destination address - 16 bits
        block2 |= (destinationAddress & INT_MASK) << 8;
        // destination sequence - 8 bits
        block2 |= destinationSequence & INT_MASK;
        output.write(toBytes(block2, 3));

        int block3 = 0;
        // originator address - 16 bits
        block3 |= (originatorAddress & INT_MASK) << 8;
        // originator sequence - 8 bits
        block3 |= originatorSequence & INT_MASK;
        output.write(toBytes(block3, 3));

        return bos.toByteArray();
    }

    public static RouteRequest parse(byte[] bytes) throws IOException {

        if (bytes.length != 9) {
            throw new RuntimeException("'Failed to parse request: Invalid length (" + bytes.length + ")");
        }

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        final DataInput input = new DataInputStream(in);

        byte[] tmp = new byte[3];

        input.readFully(tmp);
        final int block1 = toInt(tmp);
        final boolean destinationSequenceUnknown = ((block1 >> 17) & 0x1) == 1;
        final int hopCount = (block1 >> 6) & 0x3F;
        final int requestId = block1 & 0x3F;

        input.readFully(tmp);
        final int block2 = toInt(tmp);
        final int destinationAddress = (block2 >> 8) & 0xFFFF;
        final int destinationSequence = block2 & 0xFF;

        input.readFully(tmp);
        final int block3 = toInt(tmp);
        final int originatorAddress = (block3 >> 8) & 0xFFFF;
        final int originatorSequence = block3 & 0xFF;

        return new RouteRequest(hopCount, requestId, destinationAddress, destinationSequence, destinationSequenceUnknown, originatorAddress, originatorSequence);
    }

    public static boolean isRouteRequest(byte[] bytes) {
        return ((bytes[0] >> 2) & 0xFF) == TYPE;
    }

    public static void main(String[] args) throws IOException {


        RouteRequest request = new RouteRequest(MAX_6_BITS, MAX_6_BITS, MAX_16_BITS, MAX_8_BITS, true, MAX_16_BITS, MAX_8_BITS);
        RouteRequest request1 = parse(request.serialize());

        request = new RouteRequest(MAX_6_BITS, MAX_6_BITS, MAX_16_BITS, MAX_8_BITS, false, MAX_16_BITS, MAX_8_BITS);
        request1 = parse(request.serialize());

        request.serialize();


    }
}
