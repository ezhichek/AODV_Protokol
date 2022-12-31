package aodv;

import java.io.*;

import static aodv.Utils.*;
import static aodv.Utils.toLong;

public class RouteReply {

    private static final int TYPE = 2;

    private final int lifetime;

    private final int destinationAddress;

    private final int destinationSequence;

    private final int originatorAddress;

    private final int hopCount;

    public RouteReply(int lifetime, int destinationAddress, int destinationSequence, int originatorAddress, int hopCount) {
        this.lifetime = validate(lifetime, 0, MAX_18_BITS);
        this.destinationAddress = validate(destinationAddress, 0, MAX_16_BITS);
        this.destinationSequence = validate(destinationSequence, 0, MAX_8_BITS);
        this.originatorAddress = validate(originatorAddress, 0, MAX_16_BITS);
        this.hopCount = validate(hopCount, 0, MAX_8_BITS);
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationSequence() {
        return destinationSequence;
    }

    public int getOriginatorAddress() {
        return originatorAddress;
    }

    public int getHopCount() {
        return hopCount;
    }

    public byte[] serialize() throws IOException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final DataOutput output = new DataOutputStream(bos);

        int block1 = 0;
        // type - 6 bits
        block1 |= (TYPE & INT_MASK) << 18;
        // lifetime - 18 bits
        block1 |= lifetime & INT_MASK;
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
        // hop count - 8 bits
        block3 |= hopCount & INT_MASK;
        output.write(toBytes(block3, 3));

        return bos.toByteArray();
    }

    public static RouteReply parse(byte[] bytes) throws IOException {

        if (bytes.length != 9) {
            throw new RuntimeException("'Failed to parse request: Invalid length (" + bytes.length + ")");
        }

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        final DataInput input = new DataInputStream(in);

        byte[] tmp = new byte[3];

        input.readFully(tmp);
        final long block1 = toLong(tmp);
        final int lifeTime = (int)((block1 >> 6) & 0x3FFFF);

        input.readFully(tmp);
        final long block2 = toLong(tmp);
        final int destinationAddress = (int)((block2 >> 8) & 0xFFFF);
        final int destinationSequence = (int)(block2 & 0xFF);

        input.readFully(tmp);
        final long block3 = toLong(tmp);
        final int originatorAddress = (int)((block3 >> 8) & 0xFFFF);
        final int hopCount = (int)(block3 & 0xFF);

        return new RouteReply(lifeTime, destinationAddress, destinationSequence, originatorAddress, hopCount);
    }

    public static boolean isRouteReply(byte[] bytes) {
        return ((bytes[0] >> 2) & 0xFF) == TYPE;
    }
}
