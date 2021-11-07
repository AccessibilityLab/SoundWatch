package com.wearable.sound.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;
import com.wearable.sound.ui.activity.MainActivity;
import com.wearable.sound.application.MainApplication;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.legacy.content.WakefulBroadcastReceiver;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    public static final String SOUND_UNSNOOZE_FROM_WATCH_PATH = "/SOUND_UNSNOOZE_FROM_WATCH_PATH";
    public static final String TAG = "AlarmReceiver";
    private static final String CONNECTED_HOST_IDS = "CONNECTED_HOST_IDS";
    private static final String SOUND_LABEL = "SOUND_LABEL";
    private Set<String> connectedHostIds;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(MainActivity.TAG, "Alarm received");
        int blockedNotificationID = intent.getIntExtra("blockedSoundId", 0);
        ((MainApplication) context.getApplicationContext()).removeBlockedSounds(blockedNotificationID);
        final String soundLabel = intent.getStringExtra(SOUND_LABEL);
        String input = intent.getStringExtra(CONNECTED_HOST_IDS);
        Log.i(TAG, "Connected host id: " + input);
        if (input != null) {
            // There is a connected phone
            this.connectedHostIds = new HashSet<>(
                    Arrays.asList(
                            input.split(",")
                    )
            );
            sendUnSnoozeSoundMessageToPhone(context, soundLabel);
        }
    }

    private void sendUnSnoozeSoundMessageToPhone(Context context, String soundLabel) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        if (connectedHostIds == null) {
            // No phone connected to send the message right now
            return;
        }
        for (String connectedHostId : connectedHostIds) {
            Log.d(TAG, "Sending unsnooze sound data to phone:" + soundLabel);
            Wearable.getMessageClient(context)
                    .sendMessage(connectedHostId, SOUND_UNSNOOZE_FROM_WATCH_PATH, soundLabel.getBytes());
        }
    }
}