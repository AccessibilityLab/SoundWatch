package com.wearable.sound.utils;

import static com.wearable.sound.utils.Constants.DEBUG_LOG;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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


    /**
     * Find the external file given the relative path and filename and return its URI
     * @param context : context
     * @param relativeDirPath   : path to dir
     * @param filenameExternal  : filename
     * @param createNew : create a new file if true and return its URI. Else, return null
     * @return
     */
    public static Uri getExternalStorageFile(Context context, String relativeDirPath,
                                             String filenameExternal, boolean createNew) {
        Uri contentUri = MediaStore.Files.getContentUri("external");
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // https://stackoverflow.com/questions/59511147/create-copy-file-in-android-q-using-mediastore/62879112#62879112
            if (DEBUG_LOG) Log.d(TAG, "Write files using MediaStore and ContentResolver");

            // Find the file to see if it exists
            String selection = MediaStore.MediaColumns.RELATIVE_PATH + "= ? and " + MediaStore.MediaColumns.DISPLAY_NAME + "= ?";
            String[] selectionArgs = new String[] { relativeDirPath, filenameExternal };
            Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null);
            if (cursor.getCount() == 0) {
                if (DEBUG_LOG) Log.d(TAG, "No file found! Created a new log file with name=" + filenameExternal);

                // No file found --> create a new file
                if (!createNew) {
                    return null;
                }
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filenameExternal);
                // https://www.freeformatter.com/mime-types-list.html
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDirPath);

                fileUri = resolver.insert(contentUri, contentValues);
            } else {
                while (cursor.moveToNext()) {
                    String fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));

                    if (fileName.equals(filenameExternal)) {
                        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                        fileUri = ContentUris.withAppendedId(contentUri, id);
                        break;
                    }
                }
            }
        } else {
            if (DEBUG_LOG) Log.d(TAG, "Write files using Environment.getExternalStoragePublicDirectory");

            File file = new File(Environment.getExternalStoragePublicDirectory(relativeDirPath), filenameExternal);
            fileUri = Uri.fromFile(file);
        }

        return fileUri;
    }
}
