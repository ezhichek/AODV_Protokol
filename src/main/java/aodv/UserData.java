package aodv;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;
import java.util.Arrays;

import static aodv.Utils.*;

public class UserData implements Message {

    private static final int TYPE = 0;

    private final int destinationAddress;

    private final byte[] data;

    public UserData(int destinationAddress, byte[] data) {
        this.destinationAddress = validate(destinationAddress, 0, MAX_16_BITS);
        this.data = data == null ? new byte[0] : data;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserData userData = (UserData)o;
        return new EqualsBuilder()
                .append(destinationAddress, userData.destinationAddress)
                .append(data, userData.data)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(destinationAddress)
                .append(data)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("destinationAddress", destinationAddress)
                .append("data-length", data != null ? data.length : 0)
                .toString();
    }

    public byte[] serialize() throws IOException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final DataOutput output = new DataOutputStream(bos);

        int block1 = 0;
        // type - 6 bits
        block1 |= (TYPE & INT_MASK) << 18;
        // destination address - 16 bits
        block1 |= (destinationAddress & INT_MASK) << 2;
        // user data - first 2 bits
        if (data != null && data.length > 0) {
            block1 |= data[0] >> 6;
        }
        output.write(toBytes(block1, 3));

        if (data != null && data.length > 0) {
            byte[] bytes = Arrays.copyOf(data, data.length);
            Utils.shiftBytesLeft(bytes, 2);
            output.write(bytes);
        }

        return bos.toByteArray();
    }

    public static UserData parse(byte[] bytes) throws IOException {

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        final DataInput input = new DataInputStream(in);

        final byte[] tmp = new byte[3];

        input.readFully(tmp);
        final int block1 = toInt(tmp);
        final int destinationAddress = (block1 >> 2) & 0xFFFF;

        final byte[] data = new byte[bytes.length - 3];
        if (data.length > 0) {
            input.readFully(data);
            Utils.shiftBytesRight(data, 2);
            // user data - first 2 bits
            data[0] |= block1 << 6;
        }

        return new UserData(destinationAddress, data);
    }

    public static boolean isUserData(byte[] bytes) {
        return ((bytes[0] >> 2) & 0xFF) == TYPE;
    }

}
