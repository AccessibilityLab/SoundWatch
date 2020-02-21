package com.wearable.sound;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String VOICE_FILE_NAME = "audiorecord.pcm";
    private static final String TAG = "ForegroundService";

    private SoundRecorder mSoundRecorder;
    private CountDownTimer mCountDownTimer;
    private static Set<String> connectedHostIds = new HashSet<>();
    @Override
    public void onCreate() {
        super.onCreate();
        mSoundRecorder = new SoundRecorder(this, VOICE_FILE_NAME);
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
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
        //do heavy work on a background thread
        //stopSelf();
        new Thread() {
            @Override
            public void run() {
                mSoundRecorder.startRecording(connectedHostIds);
            }
        }.start();
        Log.d(TAG, "Start Recording...");
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping foreground services");
        if (mSoundRecorder != null) {
            Log.i(TAG, "Stop recorder");
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
