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
        buffer.flip();//need flip
        return buffer.getLong();
    }

    /**
     * Code to extract audio features
     */
    public static double db(short[] data) {
        double rms = 0.0;
        for (int i = 0; i < data.length; i++) {
            rms += Math.abs(data[i]);
        }
        rms = rms/data.length;
        return 20 * Math.log10(rms/32768.0);
    }

    public static double db(List<Short> soundBuffer) {
        double rms = 0.0;
        for (int i = 0; i < soundBuffer.size(); i++) {
            rms += Math.abs(soundBuffer.get(i));
        }
        rms = rms/soundBuffer.size();
        return 20 * Math.log10(rms/32768.0);
    }

    /**
     *
     * @param bytes
     * @return
     */
    public static short[] convertByteArrayToShortArray(byte[] bytes) {
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    private String convertSetToCommaSeparatedList(Set<String> connectedHostIds) {
        StringBuilder result = new StringBuilder();
        for (String connectedHostId: connectedHostIds) {
            result.append(connectedHostId);
        }
        if (connectedHostIds.size() <= 1) {
            return result.toString();
        }
        result.substring(0, result.length() - 1);
        return result.toString();
    }
}
