package com.wearable.sound.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;

public class HelperUtils {
    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();  // need flip
        return buffer.getLong();
    }

    /**
     * Calculate loudness of the audio raw shorts
     */
    public static double db(short[] data) {
        double rms = 0.0;
        int dataLength = 0;
        for (short datum : data) {
            if (datum != 0) {
                dataLength++;
            }
            rms += datum * datum;
        }
        rms = rms / dataLength;
        return 10 * Math.log10(rms);
    }

    /**
     * @param bytes an array of byte
     * @return an array of short
     */
    public static short[] convertByteArrayToShortArray(byte[] bytes) {
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    private String convertSetToCommaSeparatedList(Set<String> connectedHostIds) {
        StringBuilder result = new StringBuilder();
        for (String connectedHostId : connectedHostIds) {
            result.append(connectedHostId);
        }
        if (connectedHostIds.size() <= 1) {
            return result.toString();
        }
        result.substring(0, result.length() - 1);
        return result.toString();
    }
}
