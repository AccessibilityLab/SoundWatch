package com.example.homesound;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(MainActivity.TAG, "Alarm received");
        int blockedNotificationID = intent.getIntExtra("blockedSoundId", 0);
        ((MyApplication) context.getApplicationContext()).removeBlockedSounds(blockedNotificationID);
    }
}