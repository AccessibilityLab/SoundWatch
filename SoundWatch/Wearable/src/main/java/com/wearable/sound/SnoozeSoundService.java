package com.wearable.sound;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//TODO HUNG 1 CONTD: DON'T THINK WE NEED SOME CODE IN THIS FILE. REMOVE

public class SnoozeSoundService extends IntentService {
    public static final String SNOOZE_SOUND = "SNOOZE_SOUND";
    public static final String SOUND_ID = "SOUND_ID";
    public static final String SOUND_LABEL = "SOUND_LABEL";
    private static final String TAG = "SnoozeSoundService";
    public static final String SNOOZE_TIME = "SNOOZE_TIME";
    public static final String SOUND_SNOOZE_FROM_WATCH_PATH = "/SOUND_SNOOZE_FROM_WATCH_PATH";
    public static final String CONNECTED_HOST_IDS = "CONNECTED_HOST_IDS";
    private static final Map<String, Integer> SNOOZE_TIME_MAP = new HashMap<>();
    private Set<String> connectedHostIds;

    {
        SNOOZE_TIME_MAP.put("5 mins", 5 * 60 * 1000);
        SNOOZE_TIME_MAP.put("10 mins", 10 * 60 * 1000);
        SNOOZE_TIME_MAP.put("1 hour", 60 * 60 * 1000);
        SNOOZE_TIME_MAP.put("1 day", 24 * 60 * 60 * 1000);
        SNOOZE_TIME_MAP.put("Forever", -1);

    }

    public SnoozeSoundService() {
        super("SnoozeSoundService");
    }

    /*
     * Extracts CharSequence created from the RemoteInput associated with the Notification.
     */
    private String getMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(SNOOZE_TIME).toString();
        }
        return "10 mins";
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent(): " + intent);

        if (intent != null) {
            String snoozeTime = getMessage(intent);
//            final int snoozeTime = intent.getIntExtra(SNOOZE_TIME, 0);
            Log.i(TAG, "Snooze time: "+ snoozeTime);
            if (snoozeTime == null) {
                return;
            }
            final int blockedNotificationID = intent.getIntExtra(SOUND_ID, 0);
            final String soundLabel = intent.getStringExtra(SOUND_LABEL);
            String input = intent.getStringExtra(CONNECTED_HOST_IDS);
            if (input != null) {
                // There is a connected phone
                final Set<String> connectedHostIds = new HashSet<>(
                        Arrays.asList(
                                input.split(",")
                        )
                );
                this.connectedHostIds = connectedHostIds;
            }


            ((MyApplication) this.getApplication()).addBlockedSounds(blockedNotificationID);
            Log.i(TAG, "Add to list of blocked sounds " + blockedNotificationID);

            if (!snoozeTime.equals("Forever")) {
                Intent alarmIntent = new Intent(this,  AlarmReceiver.class);
                alarmIntent.setAction("com.wearable.sound.almMgr");
                alarmIntent.putExtra("blockedSoundId", blockedNotificationID);
                int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, uniqueInt, alarmIntent, 0);
                AlarmManager alarmMgr = (AlarmManager)this.getSystemService(ALARM_SERVICE);
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SNOOZE_TIME_MAP.getOrDefault(snoozeTime, 0), pendingIntent);
            }

            //Remove all notifications of this sound in the list
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(blockedNotificationID);

            Log.i(TAG, "Successfully blocked sounds");

            // Send a message to Phone to indicate this sound is blocked
            sendSnoozeSoundMessageToPhone(soundLabel);


        }
    }

    private void sendSnoozeSoundMessageToPhone(String soundLabel) {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bas);
            if (connectedHostIds == null) {
                // No phone connected to send the message right now
                return;
            }
            for (String connectedHostId : connectedHostIds) {
                Log.d(TAG, "Sending snooze sound data to phone:" + soundLabel);
                Task<Integer> sendMessageTask =
                        Wearable.getMessageClient(this.getApplicationContext())
                                .sendMessage(connectedHostId, SOUND_SNOOZE_FROM_WATCH_PATH, soundLabel.getBytes());
            }
    }
}
