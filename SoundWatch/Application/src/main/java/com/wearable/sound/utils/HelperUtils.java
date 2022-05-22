package com.wearable.sound.utils;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Containing helper methods
 */
public class HelperUtils {
    private static final String TAG = "Phone/HelperUtils";

    public static short[] convertByteArrayToShortArray(byte[] bytes) {
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    public static float[] convertByteArrayToFloatArray(byte[] buffer) {
        try {
            ByteArrayInputStream bas = new ByteArrayInputStream(buffer);
            DataInputStream ds = new DataInputStream(bas);
            float[] fArr = new float[buffer.length / 4];  // 4 bytes per float
            for (int i = 0; i < fArr.length; i++) {
                fArr[i] = ds.readFloat();
            }
            return fArr;
        } catch (IOException e) {
            if (Constants.DEBUG_LOG) Log.e(TAG, "ERROR parsing bytes array to float array");
        }
        return null;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip(); // need flip
        return buffer.getLong();
    }

    /**
     * Calculate the loudness in db of an array of raw audio shorts
     *
     * @param data : an array of shorts representing the audio shorts
     * @return the loudness in db
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
}
