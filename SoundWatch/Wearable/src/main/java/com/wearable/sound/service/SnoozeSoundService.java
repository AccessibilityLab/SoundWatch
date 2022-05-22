package com.wearable.sound.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.wearable.Wearable;
import com.wearable.sound.utils.AlarmReceiver;
import com.wearable.sound.application.MainApplication;

import static com.wearable.sound.utils.Constants.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.wearable.sound.ui.activity.MainActivity.convertSetToCommaSeparatedList;


public class SnoozeSoundService extends IntentService {

    private static final String TAG = "SnoozeSoundService";

    private Set<String> connectedHostIds;

    public SnoozeSoundService() {
        super("SnoozeSoundService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (DEBUG_LOG) {
            Log.d(TAG, "onHandleIntent(): " + intent);
        }

        if (intent != null) {
            String snoozeTime = "10 mins";
            final int blockedNotificationID = intent.getIntExtra(SOUND_ID, 0);
            final String soundLabel = intent.getStringExtra(SOUND_LABEL);
            String input = intent.getStringExtra(CONNECTED_HOST_IDS);
            if (input != null) {
                // There is a connected phone
                this.connectedHostIds = new HashSet<>(
                        Arrays.asList(
                                input.split(",")
                        )
                );
            }
            ((MainApplication) this.getApplication()).addBlockedSounds(blockedNotificationID);
            if (DEBUG_LOG) Log.i(TAG, "Add to list of blocked sounds " + blockedNotificationID);

            if (!snoozeTime.equals("Forever")) {
                Intent alarmIntent = new Intent(this, AlarmReceiver.class);
                alarmIntent.setAction("com.wearable.sound.almMgr");
                alarmIntent.putExtra("blockedSoundId", blockedNotificationID);
                alarmIntent.putExtra(SOUND_LABEL, soundLabel);
                alarmIntent.putExtra(CONNECTED_HOST_IDS, convertSetToCommaSeparatedList(connectedHostIds));
                int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, uniqueInt, alarmIntent, 0);
                AlarmManager alarmMgr = (AlarmManager) this.getSystemService(ALARM_SERVICE);
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 60 * 1000, pendingIntent);
            }

            //Remove all notifications of this sound in the list
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(blockedNotificationID);

            if (DEBUG_LOG) Log.i(TAG, "Successfully blocked sounds");

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
            if (DEBUG_LOG) Log.d(TAG, "Sending snooze sound data to phone: " + soundLabel);
            Wearable.getMessageClient(this.getApplicationContext())
                    .sendMessage(connectedHostId, SOUND_SNOOZE_FROM_WATCH_PATH, soundLabel.getBytes());
        }
    }
}
