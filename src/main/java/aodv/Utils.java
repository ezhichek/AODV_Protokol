package aodv;

public class Utils {

    public static final int ACTIVE_ROUTE_TIMEOUT = 3000;
    public static final int MY_ROUTE_TIMEOUT = 2 * ACTIVE_ROUTE_TIMEOUT;
    public static final int NET_DIAMETER = 35;
    public static final int NODE_TRAVERSAL_TIME = 40;
    public static final int NET_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME * NET_DIAMETER;
    public static final int RREQ_RETRIES = 2;

    public static final int INT_MASK = 0xffffffff;
    public static final int MAX_6_BITS = 64 - 1;
    public static final int MAX_8_BITS = 256 - 1;
    public static final int MAX_16_BITS = 65536 - 1;
    public static final int MAX_18_BITS = 262144 - 1;

    public static final int BROADCAST_ADDRESS = 0xFFFF;


    public static byte[] toBytes(int value, int length) {
        if (value < 0 || value > Math.pow(2, length * 8)) {
            throw new RuntimeException("Invalid value");
        }
        if (length < 1) {
            throw new RuntimeException("Invalid length");
        }
        final byte[] bytes = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            bytes[i] = (byte)(value & 0xFF);
            value >>= Byte.SIZE;
        }
        return bytes;
    }

    public static int toInt(byte[] bytes) {
        final int length = bytes.length;
        if (length == 0 || length > 8) {
            throw new RuntimeException("Invalid bytes");
        }
        int value = 0;
        for (int i = 0; i < length; i++) {
            value <<= Byte.SIZE;
            value |= (bytes[i] & 0xFF);
        }
        return value;
    }

    public static void shiftBytesLeft(byte[] bytes, int positions) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] <<= positions;
            if (i < bytes.length - 1) {
                bytes[i] |= ((bytes[i + 1] & 0xFF) >> (Byte.SIZE - positions));
            }
        }
    }

    public static void shiftBytesRight(byte[] bytes, int positions) {
        for (int i = bytes.length - 1; i >= 0; i--) {
            bytes[i] = (byte)((bytes[i] & 0xFF) >> positions);
            if (i > 0) {
                bytes[i] |= (bytes[i - 1] << (Byte.SIZE - positions));
            }
        }
    }

    public static int validate(int value, int min, int max) {
        if (value < min || value > max) {
            throw new RuntimeException("Value exceeds limits (value: " + value + ", min: " + min + ", max: " + max + ")");
        }
        return value;
    }

    public static void main(String[] args) {

        final String s = "Hallo Harald";

        byte[] bytes = s.getBytes();

        print(bytes);
        shiftBytesLeft(bytes, 2);
        System.out.println(new String(bytes));
        print(bytes);
        shiftBytesRight(bytes, 2);
        print(bytes);
        System.out.println(new String(bytes));


        System.out.println(toInt(toBytes(1, 6)));
        System.out.println(toInt(toBytes(10, 6)));
        System.out.println(toInt(toBytes(100, 6)));
        System.out.println(toInt(toBytes(1000, 6)));
        System.out.println(toInt(toBytes(10000, 6)));
        System.out.println(toInt(toBytes(100000, 6)));
        System.out.println(toInt(toBytes(1000000, 6)));
        System.out.println(toInt(toBytes(10000000, 6)));
        System.out.println(toInt(toBytes(100000000, 6)));
        System.out.println(toInt(toBytes(1000000000, 6)));
    }

    private static void print(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            System.out.print(toBinaryString(bytes[i]) + " ");
        }
        System.out.println();
    }

    private static String toBinaryString(byte b) {
        String s = "";
        s += (b & 0b10000000) > 0 ? "1" : "0";
        s += (b & 0b01000000) > 0 ? "1" : "0";
        s += (b & 0b00100000) > 0 ? "1" : "0";
        s += (b & 0b00010000) > 0 ? "1" : "0";
        s += (b & 0b00001000) > 0 ? "1" : "0";
        s += (b & 0b00000100) > 0 ? "1" : "0";
        s += (b & 0b00000010) > 0 ? "1" : "0";
        s += (b & 0b00000001) > 0 ? "1" : "0";
        return s;
    }


}
