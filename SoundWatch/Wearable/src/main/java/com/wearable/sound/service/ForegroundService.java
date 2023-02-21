package com.wearable.sound.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import com.wearable.sound.ui.activity.MainActivity;
import com.wearable.sound.R;
import com.wearable.sound.utils.Constants;
import com.wearable.sound.utils.SoundRecorder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.core.app.NotificationCompat;

import static com.wearable.sound.utils.Constants.*;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";

    private SoundRecorder mSoundRecorder;
    private CountDownTimer mCountDownTimer;
    private final Set<String> connectedHostIds = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.DEBUG_LOG) Log.i(TAG, "onCreate");
        mSoundRecorder = new SoundRecorder(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("connectedHostIds");
        final Set<String> connectedHostIds = new HashSet<>(
                Arrays.asList(
                        input.split(",")
                )
        );
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Listening...")
                .setSmallIcon(R.drawable.ic_baseline_surround_sound_24)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
        mSoundRecorder.startRecording(connectedHostIds);
        if (Constants.DEBUG_LOG) Log.d(TAG, "Start Recording...");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Constants.DEBUG_LOG) Log.i(TAG, "Stopping foreground services");
        if (mSoundRecorder != null) {
            if (Constants.DEBUG_LOG) Log.i(TAG, "Stop recorder");
            mSoundRecorder.stopRecording();
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
