package com.example.homesound;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity implements WearableListView.ClickListener, WearableListView.OnCentralPositionChangedListener {

    public static final String TAG = "HomeSoundDebug";
    private String intentLocation = "";
    private String intentSound = "";
    private String intentConfidence = "";
    private int wearableListCentralPosition = 1;
    String[] elements = {"1 min", "2 mins", "5 mins", "10 mins", "1 hour", "1 day", "Forever"};
    int[] elementsInSec = {60,      120,        300,        600,    3600,   86400};

@Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        //Start an always running background service
        startService(new Intent(this, BackgroundService.class));

        //Register to a topic
        FirebaseMessaging.getInstance().subscribeToTopic("port_" + ((MyApplication) this.getApplication()).SERVER_PORT)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    String msg = "Subscribed to firebase topic port_" + ((MyApplication) getApplicationContext()).SERVER_PORT;
                    if (!task.isSuccessful()) {
                        msg = "Firebase topic subscribing failed";
                    }
                    Log.d(TAG, msg);
                }
            });

        // Set the UI
        setContentView(R.layout.activity_main);
        (findViewById(R.id.wearable_list_layout)).setVisibility(View.GONE);

        //Just in case the app has exited midway and started again from an intent, get data from PendingIntent
        String [] dataPassed = {"", "", ""};
        dataPassed = this.getIntent().getStringArrayExtra(MyFirebaseMessagingService.INTENT_KEY);
        if (dataPassed != null)
        {
            intentLocation = dataPassed[0];
            intentSound = dataPassed[1];
            intentConfidence = dataPassed[2];

            TextView soundDisplay = findViewById(R.id.soundDisplay);
            TextView locationDisplay = findViewById(R.id.locationDisplay);
            locationDisplay.setText(intentLocation);
            soundDisplay.setText(intentSound + ", " + (int)Math.round(Float.parseFloat(intentConfidence)*100) + "%");
            (findViewById(R.id.dontshowDisplay_layout)).setVisibility(View.VISIBLE);
            (findViewById(R.id.wearable_list_layout)).setVisibility(View.VISIBLE);
            TextView fTextView = (findViewById(R.id.dontshowDisplay));
            fTextView.setText("Don't notify this sound for:");
        }

        // Get the list component from the layout of the activity
        WearableListView listView =
                (WearableListView) findViewById(R.id.wearable_list);

        // Assign an adapter to the list
        listView.setAdapter(new Adapter(this, elements));

        // Set a click listener
        listView.setClickListener(this);

        //Scroll to the second position to fill the empty gap up top.
        listView.scrollToPosition(wearableListCentralPosition);

        // Set a central position change listener
        listView.addOnCentralPositionChangedListener(this);

        // Enables Always-on - I don't need the foreground to be always on, I just need network access and the app to generate notification during doze.
        //setAmbientEnabled();
    }

    @Override
    protected void onPause(){   //First indication that user is leaving your activity -- doesn't mean the activity is destroyed
        Log.d(TAG, "onPause()");
        ((MyApplication) this.getApplication()).setAppInForeground(false);
        super.onPause();
    }
    @Override
    protected void onResume(){   //App comes to the foreground until user focus is taking away, when onPause is called

        Log.d(TAG, "onResume()");
        ((MyApplication) this.getApplication()).setAppInForeground(true);
        super.onResume();
    }
    @Override
    protected void onStart(){   //This method is where the app initializes the code that maintains the UI.

        Log.d(TAG, "onStart()");
        super.onStart();
    }
    @Override
    protected void onStop(){

        Log.d(TAG, "onStop()");
        ((MyApplication) this.getApplication()).setAppInForeground(false);
        super.onStop();
    }
    @Override
    protected void onDestroy(){

        Log.d(TAG, "onDestroy()");
        ((MyApplication) this.getApplication()).setAppInForeground(false);
        super.onDestroy();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent()");

        //Get the 'extra' from the intent
        String [] dataPassed = {"", "", ""};
        dataPassed = intent.getStringArrayExtra(MyFirebaseMessagingService.INTENT_KEY);
        if (dataPassed != null)
        {
            intentLocation = dataPassed[0];
            intentSound = dataPassed[1];
            intentConfidence = dataPassed[2];

            TextView soundDisplay = findViewById(R.id.soundDisplay);
            TextView locationDisplay = findViewById(R.id.locationDisplay);
            locationDisplay.setText(intentLocation);
            soundDisplay.setText(intentSound + ", " + (int)Math.round(Float.parseFloat(intentConfidence)*100) + "%");
            (findViewById(R.id.dontshowDisplay_layout)).setVisibility(View.VISIBLE);
            (findViewById(R.id.wearable_list_layout)).setVisibility(View.VISIBLE);
            TextView fTextView = (findViewById(R.id.dontshowDisplay));
            fTextView.setText("Don't notify this sound for:");
        }
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        Integer tag = (Integer) v.itemView.getTag();

        if(tag!=wearableListCentralPosition)
            return;

        if(tag <= elementsInSec.length)     //Just an extra check
        {
            final int blockedNotificationID = ((MyApplication) getApplicationContext()).getIntegerValueOfSound(intentSound + intentLocation);

            if((((MyApplication) this.getApplication()).getBlockedSounds()).contains(blockedNotificationID)) {         //Just in case the sound is already blocked and user comes to it again.
                (findViewById(R.id.wearable_list_layout)).setVisibility(View.GONE);
                TextView fTextView = (findViewById(R.id.dontshowDisplay));
                fTextView.setText("This sound is already blocked.");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView soundDisplay = findViewById(R.id.soundDisplay);
                                TextView locationDisplay = findViewById(R.id.locationDisplay);
                                locationDisplay.setText("");
                                soundDisplay.setText("HomeSounds app");
                                TextView fTextView = (findViewById(R.id.dontshowDisplay));
                                fTextView.setText("Press the side button and wait for notifications.");
                            }
                        });

                        moveTaskToBack(true);
                    }
                }, 3000);
                return;
            }

            ((MyApplication) this.getApplication()).addBlockedSounds(blockedNotificationID);

            if (tag < elementsInSec.length)     //If not forever, start an alarm manager to remove sounds from blocked list. AlarmManager because Doze mode..
            {
                Intent alarmIntent = new Intent(this,  AlarmReceiver.class);
                alarmIntent.setAction("com.example.homesound.almMgr");
                alarmIntent.putExtra("blockedSoundId", blockedNotificationID);
                int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, uniqueInt, alarmIntent, 0);
                AlarmManager alarmMgr = (AlarmManager)this.getSystemService(ALARM_SERVICE);
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + elementsInSec[tag]*1000, pendingIntent);
            }

            //Remove all notifications of this sound in the list
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(blockedNotificationID);

            //Change view to avoid reclicking and also to give feedback
            (findViewById(R.id.wearable_list_layout)).setVisibility(View.GONE);
            TextView fTextView = (findViewById(R.id.dontshowDisplay));
            fTextView.setText("This sound is blocked for " + elements[tag] + ".");
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView soundDisplay = findViewById(R.id.soundDisplay);
                            TextView locationDisplay = findViewById(R.id.locationDisplay);
                            locationDisplay.setText("");
                            soundDisplay.setText("HomeSounds app");
                            TextView fTextView = (findViewById(R.id.dontshowDisplay));
                            fTextView.setText("Press the side button and wait for notifications.");
                        }
                    });

                    moveTaskToBack(true);
                }
            }, 3000);
        }
    }

    @Override
    public void onCentralPositionChanged(int centralPosition) {
        wearableListCentralPosition = centralPosition;
    }

    private static final class Adapter extends WearableListView.Adapter {
        private String[] mDataset;
        private final Context mContext;
        private final LayoutInflater mInflater;

        // Provide a suitable constructor (depends on the kind of dataset)
        public Adapter(Context context, String[] dataset) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        // Provide a reference to the type of views you're using
        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;
            public ItemViewHolder(View itemView) {
                super(itemView);
                // find the text view within the custom item's layout
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        // Create new views for list items
        // (invoked by the WearableListView's layout manager)
        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            // Inflate our custom layout for list items
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
        }

        // Replace the contents of a list item
        // Instead of creating new views, the list tries to recycle existing ones
        // (invoked by the WearableListView's layout manager)
        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder,
                                     int position) {
            // retrieve the text view
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            // replace text contents
            view.setText(mDataset[position]);
            // replace list item's metadata
            holder.itemView.setTag(position);
        }

        // Return the size of your dataset
        // (invoked by the WearableListView's layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }

    }
}
