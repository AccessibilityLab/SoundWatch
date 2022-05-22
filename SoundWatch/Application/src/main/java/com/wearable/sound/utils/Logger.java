package com.wearable.sound.utils;

import static com.wearable.sound.utils.Constants.DEBUG_LOG;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.wearable.sound.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    /**
     * For the field study logging: log with firebase and also write to an external file
     */
    public static synchronized void fsLogging(Context context,
                                              FirebaseAnalytics mFirebaseAnalytics,
                                              String tag,
                                              String eventName,
                                              Bundle bundle,
                                              String ts) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);

        // Building the logging bundle (key/value pairs)
        // always add participant id and current shard pref
        String pidKey = context.getResources().getString(R.string.pid_pref_key);
        String participantId = sharedPref.getString(pidKey, Build.MODEL);
        bundle.putString(pidKey, participantId);
        String dbLevelKey = context.getResources().getString(R.string.db_threshold_pref_key);
        bundle.putInt(dbLevelKey, sharedPref.getInt(dbLevelKey, 40));
        String fgPrefKey = context.getResources().getString(R.string.foreground_pref_key);
        bundle.putBoolean(fgPrefKey, sharedPref.getBoolean(fgPrefKey, false));
        bundle.putString("event_name", eventName);
        bundle.putString("ts", ts);

        // log with firebase
        mFirebaseAnalytics.logEvent(eventName, bundle);

        // write to external file
        String state = Environment.getExternalStorageState();
        // Check the availability of the external storage
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            if (DEBUG_LOG) Log.i(tag, "External storage is not mounted to write");
            return;
        }
        String filenameExternal = "SWFS22-" + participantId + "-" + tag + "Log.txt";
        filenameExternal = filenameExternal.replace("/", "-");
        String relativeDir = Environment.DIRECTORY_DOCUMENTS + "/SoundWatch/";
        OutputStream outputStream = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // https://stackoverflow.com/questions/59511147/create-copy-file-in-android-q-using-mediastore/62879112#62879112
            if (DEBUG_LOG) Log.d(tag, "Write files using MediaStore and ContentResolver");

            // Find the file to see if it exists
            Uri contentUri = MediaStore.Files.getContentUri("external");
            String selection = MediaStore.MediaColumns.RELATIVE_PATH + "= ? and " + MediaStore.MediaColumns.DISPLAY_NAME + "= ?";
            String[] selectionArgs = new String[] { relativeDir, filenameExternal };
            Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null);
            Uri fileUri = null;
            if (cursor.getCount() == 0) {
                if (DEBUG_LOG) Log.d(tag, "No file found! Created a new log file with name=" + filenameExternal);

                // No file found --> create a new file
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filenameExternal);
                // https://www.freeformatter.com/mime-types-list.html
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir);

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

            try {
                outputStream = context.getContentResolver().openOutputStream(fileUri, "wa");
            } catch (Exception e) {
                if (DEBUG_LOG) Log.e(tag, e.toString());
                return;
            }
        } else {
            if (DEBUG_LOG) Log.d(tag, "Write files using Environment.getExternalStoragePublicDirectory");

            File file = new File(Environment.getExternalStoragePublicDirectory(relativeDir), filenameExternal);
            try {
                outputStream = new FileOutputStream(file, true);
            } catch (Exception e) {
                if (DEBUG_LOG) Log.e(tag, e.toString());
                return;
            }
        }
        try {
            if (DEBUG_LOG) Log.d(tag, "Write content to file=" + filenameExternal);
            outputStream.write(getBundleString(bundle).getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            if (DEBUG_LOG) Log.e(tag, e.toString());
        }
    }

    /**
     * Return the string representative of the bundle
     */
    public static String getBundleString(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        char separator = ' ';
        for (String key: bundle.keySet()) {
            sb.append(separator).append(key).append("=").append(bundle.get(key));
            separator = ',';
        }
        sb.append(" }\n");

        return sb.toString();
    }
}
