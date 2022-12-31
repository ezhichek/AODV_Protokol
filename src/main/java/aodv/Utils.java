package aodv;

public class Utils {


    public static final int INT_MASK = 0xffffffff;
    public static final int MAX_6_BITS = 64 - 1;
    public static final int MAX_8_BITS = 256 - 1;
    public static final int MAX_16_BITS = 65536 - 1;
    public static final int MAX_18_BITS = 262144 - 1;

    public static byte[] toBytes(long value, int length) {
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
        return (int)toLong(bytes);
    }

    public static long toLong(byte[] bytes) {
        final int length = bytes.length;
        if (length == 0 || length > 8) {
            throw new RuntimeException("Invalid bytes");
        }
        long value = 0;
        for (int i = 0; i < length; i++) {
            value <<= Byte.SIZE;
            value |= (bytes[i] & 0xFF);
        }
        return value;
    }


    public static void main(String[] args) {


        System.out.println(toLong(toBytes(1l, 6)));
        System.out.println(toLong(toBytes(10l, 6)));
        System.out.println(toLong(toBytes(100l, 6)));
        System.out.println(toLong(toBytes(1000l, 6)));
        System.out.println(toLong(toBytes(10000l, 6)));
        System.out.println(toLong(toBytes(100000l, 6)));
        System.out.println(toLong(toBytes(1000000l, 6)));
        System.out.println(toLong(toBytes(10000000l, 6)));
        System.out.println(toLong(toBytes(100000000l, 6)));
        System.out.println(toLong(toBytes(1000000000l, 6)));
        System.out.println(toLong(toBytes(10000000000l, 6)));


        System.out.println(toLong(toBytes(1000000l, 6)));

    }


    static void print(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            //System.out.print(String.format("%08s ", bytes[i]));
            System.out.print(Integer.toBinaryString(bytes[i]));
            System.out.print(" ");
        }
        System.out.println();
    }

    public static int validate(int value, int min, int max) {
        if (value < min || value > max) {
            throw new RuntimeException("Value exceeds limits (value: " + value + ", min: " + min + ", max: " + max + ")");
        }
        return value;
    }

    public static long validate(long value, long min, long max) {
        if (value < min || value > max) {
            throw new RuntimeException("Value exceeds limits (value: " + value + ", min: " + min + ", max: " + max + ")");
        }
        return value;
    }
}
