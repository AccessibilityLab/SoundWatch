package com.example.homesound;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static com.example.homesound.MainActivity.TAG;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "HomeSoundChannel888";
    public static final String INTENT_KEY = "HomeSoundIntent888";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Firebase message data payload: " + remoteMessage.getData());
            generateBigTextStyleNotification(remoteMessage.getData().get("location"), remoteMessage.getData().get("sound"), remoteMessage.getData().get("confidence"));
        }
    }

    @Override
    public void onDeletedMessages() {
        Log.d(TAG, "Firebase OnDeletedMessages called");
    }

    private void generateBigTextStyleNotification(String location, String predictedSound, String confidenceLevel) {

        final int NOTIFICATION_ID = ((MyApplication) getApplicationContext()).getIntegerValueOfSound(predictedSound + location);

        //If it's blocked or app in foreground (to not irritate user), return
        if ((((MyApplication) this.getApplication()).getBlockedSounds()).contains(NOTIFICATION_ID)
                || ((MyApplication) this.getApplication()).isAppInForeground())
            return;

        Log.d(TAG, "generateBigTextStyleNotification()");

        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);       //Just go the MainActivity for now. Replace with other activity if you want more actions.
        String[] dataPassed = {location, predictedSound, confidenceLevel};         //Adding data to be passed back to the main activity
        intent.putExtra(INTENT_KEY, dataPassed);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //try FLAG_NO_CREATE, FLAG_IMMUTABLE

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(location)
                .setContentText(predictedSound + ", " + (int) Math.round(Float.parseFloat(confidenceLevel) * 100) + "%")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(predictedSound + ", " + (int) Math.round(Float.parseFloat(confidenceLevel) * 100) + "%")
                        .setBigContentTitle(location)
                        .setSummaryText("This summary text does not show up"))
                .setAutoCancel(true) //Remove notification from the list after the user has tapped it
                .setContentIntent(pendingIntent);
        //.setOnlyAlertOnce(true);     //Only notify once (see notification ID below)
        //.addAction(snoozeAction)
        //.addAction(dismissAction);

        //JUGAAD: NOTIFICATION ID depends on the sound and the location so a particular sound in a particular location is only notified once until dismissed
        Log.d(TAG, "Notification Id: " + NOTIFICATION_ID);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notificationCompatBuilder.build());
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(350);

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Home Sound";
            String description = "Home Sound app channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MAX);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Integer.parseInt("1");
        }
    }

    /*Helper methods*/
}
