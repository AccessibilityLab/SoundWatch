package com.wearable.sound;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

public class MainActivity extends WearableActivity implements WearableListView.ClickListener, WearableListView.OnCentralPositionChangedListener {
    public static final boolean TEST_MODEL_LATENCY = false;
    public static final boolean TEST_E2E_LATENCY = false;
    private static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
    private static final String DEFAULT_SERVER = "http://128.208.49.41:8788";

    /**
     * Sound or sound features send configuration
     */
    public static final String RAW_AUDIO_TRANSMISSION = "RAW_AUDIO_TRANSMISSION";
    public static final String AUDIO_FEATURES_TRANSMISSION = "AUDIO_FEATURES_TRANSMISSION";
    public static final String AUDIO_TRANMISSION_STYLE = AUDIO_FEATURES_TRANSMISSION;

    /**
     * Architecture configurations
     */
    public static final String PHONE_WATCH_ARCHITECTURE = "PHONE_WATCH_ARCHITECTURE";
    public static final String PHONE_WATCH_SERVER_ARCHITECTURE = "PHONE_WATCH_SERVER_ARCHITECTURE";
    public static final String WATCH_ONLY_ARCHITECTURE = "WATCH_ONLY_ARCHITECTURE";
    public static final String WATCH_SERVER_ARCHITECTURE = "WATCH_SERVER_ARCHITECTURE";
    public static final String ARCHITECTURE = PHONE_WATCH_ARCHITECTURE;


    /**
     * Notification configurations
     */

    public static boolean notificationChannelIsCreated = false;
    public static final String TAG = "HomeSoundDebug";
    private String audioLabel = "";
    private String confidence = "";
    private String audioTime = "";
    private String db = "";
    private int wearableListCentralPosition = 1;
    String[] elements = {"1 min", "2 mins", "5 mins", "10 mins", "1 hour", "1 day", "Forever"};
    int[] elementsInSec = {60,      120,        300,        600,    3600,   86400};
    private static final String CHANNEL_ID = "SOUNDWATCH";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static Set<String> connectedHostIds = new HashSet<>();
    //private Map<String, Long> soundLastTime = new HashMap<>();

    //Notification snooze configurations
    private Map<String, Long> soundLastTime = new HashMap<>();
    // More notification snooze choices
    //private static final String SNOOZE_LABEL = "Snooze";
    //private static final String SNOOZE_TIME_LABEL = "Snooze Time";
    //private static final String[] SNOOZE_CHOICES = {"5 mins", "10 mins", "1 hour", "1 day", "Forever"};

    /**
     * Broadcast services
     */
    public static final String mBroadcastSoundPrediction = "com.wearable.sound.broadcast.soundprediction";
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mBroadcastSoundPrediction)) {
                String data = intent.getStringExtra(DataLayerListenerService.AUDIO_LABEL_FROM_PHONE);
                Log.i(TAG, "Received audio data from phone: " + data);
                String[] parts = data.split(",");
                // TODO: Ask DJ again, this looks really ugly
                if (TEST_E2E_LATENCY) {
                    if (parts.length == 5) {
                        String prediction = parts[0];
                        String confidence = parts[1];
                        String time = parts[2];
                        String db = parts[3];
                        String recordTime = parts[4];
                        createAudioLabelNotification(new AudioLabel(prediction, confidence, time, db, recordTime));
                    }
                } else {
                    if (parts.length == 4) {
                        String prediction = parts[0];
                        String confidence = parts[1];
                        String time = parts[2];
                        String db = parts[3];
                        createAudioLabelNotification(new AudioLabel(prediction, confidence, time, db, null));
                    }
                }
            }
        }
    };


    /**
     * Recording
     */

    public static boolean IS_RECORDING = false;

    /**
     * SocketIO
     * @return
     */

    public static Socket mSocket;
    {
        String SERVER_URL = DEFAULT_SERVER;
        if(TEST_E2E_LATENCY)
            SERVER_URL = TEST_E2E_LATENCY_SERVER;

        try {
            mSocket = IO.socket(SERVER_URL);
        } catch (URISyntaxException e) {}
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i(TAG, "Received socket event");
            JSONObject data = (JSONObject) args[0];
            String db;
            String audio_label;
            String accuracy;
            String record_time;
            try {
                audio_label = data.getString("label");
                accuracy = data.getString("accuracy");
                db = data.getString("db");
                record_time = data.getString("record_time");
            } catch (JSONException e) {
                return;
            }
            Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
            AudioLabel audioLabel;
            if (TEST_E2E_LATENCY) {
                audioLabel = new AudioLabel(audio_label, accuracy, java.time.LocalTime.now().toString(), db,
                        record_time);
            } else {
                audioLabel = new AudioLabel(audio_label, accuracy, java.time.LocalTime.now().toString(), db,
                        null);

            }
            long test = System.currentTimeMillis();
            if (soundLastTime.containsKey(audio_label)) {
                if (test <= (soundLastTime.get(audio_label) + 15 * 1000)) { //multiply by 1000 to get milliseconds
                    Log.i(TAG, "Same sound appear in less than 15 seconds");
                    return; // stop sending noti if less than 10 seconds
                }
            }
            soundLastTime.put(audio_label, test);
            createAudioLabelNotification(audioLabel);
        }
    };

    private Emitter.Listener onEchoMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i(TAG, "Received echo event");
        }
    };

    /**
     * Checks the permission that this app needs and if it has not been granted, it will
     * prompt the user to grant it, otherwise it shuts down the app.
     */
    private void checkPermissions() {
        boolean recordAudioPermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED;
        if (recordAudioPermissionGranted) {

        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                // Permission has been denied before. At this point we should show a dialog to
                // user and explain why this permission is needed and direct him to go to the
                // Permissions settings for the app in the System settings. For this sample, we
                // simply exit to get to the important part.
                Toast.makeText(this, "Need Audio access to start streaming and recognizing surrounding voices", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription(CHANNEL_ID);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mSocket.on("audio_label", onNewMessage);
        // mSocket.on("echo", onEchoMessage);
        mSocket.connect();

        // Set the UI
        setContentView(R.layout.activity_main);
        (findViewById(R.id.wearable_list_layout)).setVisibility(View.GONE);

        //Just in case the app has exited midway and started again from an intent, get data from PendingIntent
        String[] dataPassed = {"", "", ""};
        dataPassed = this.getIntent().getStringArrayExtra("audio_label");
        if (dataPassed != null)
        {
            audioLabel = dataPassed[0];
            confidence = dataPassed[1];
            audioTime = dataPassed[2];
            db = dataPassed[3];

            TextView soundDisplay = findViewById(R.id.soundDisplay);
            TextView locationDisplay = findViewById(R.id.locationDisplay);
            locationDisplay.setText(audioLabel + "(" + db + " dB");
            soundDisplay.setText(confidence + ", " + (int) Math.round(Float.parseFloat(audioTime)*100));
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


        //Enables Always-on - I don't need the foreground to be always on, I just need network access and the app to generate notification during doze.
        //setAmbientEnabled();
        checkPermissions();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcastSoundPrediction);
        registerReceiver(mReceiver, mIntentFilter);
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
        mSocket.disconnect();
        mSocket.off("audio_label", onNewMessage);
    }

    public void onRecordClick(View view) {
        if (!IS_RECORDING) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.putExtra("connectedHostIds", convertSetToCommaSeparatedList(connectedHostIds));
            ContextCompat.startForegroundService(this, serviceIntent);

            // Change the image to STOP icon
            ImageView imageView = findViewById(R.id.mic);
            imageView.setBackground(getResources().getDrawable(R.drawable.rounded_background_red));
            imageView.setImageResource(R.drawable.ic_pause_black_32dp);

            // Change the instruction text
            TextView textView = findViewById(R.id.dontshowDisplay);
            textView.setText("Press side button and wait for notifications");
            IS_RECORDING = true;
        } else {
            ImageView imageView = findViewById(R.id.mic);
            imageView.setBackground(getResources().getDrawable(R.drawable.rounded_background_blue));
            imageView.setImageResource(R.drawable.ic_mic_32dp);
            IS_RECORDING = false;
            onStopClick(view);

            // Change the instruction text
            TextView textView = findViewById(R.id.dontshowDisplay);
            textView.setText("Click the button above \n to begin listening");
        }

    }

    public void onStopClick(View view) {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    private String convertSetToCommaSeparatedList(Set<String> connectedHostIds) {
        StringBuilder result = new StringBuilder();
        for (String connectedHostId: connectedHostIds) {
            result.append(connectedHostId);
        }
        if (connectedHostIds.size() <= 1) {
            return result.toString();
        }
        result.substring(0, result.length() - 1);
        return result.toString();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent()");

        //Get the 'extra' from the intent
        String[] dataPassed = intent.getStringArrayExtra("audio_label");
        if (dataPassed != null)
        {
            audioLabel = dataPassed[0];
            confidence = dataPassed[1];
            audioTime = dataPassed[2];
            db = dataPassed[3];
            if (db.contains("\\.")) {
                String[] parts = db.split("\\.");
                db = parts[0];
            } else {
                db = db.substring(0, 2);
            }

            String[] times = audioTime.split(":");

            TextView soundDisplay = findViewById(R.id.soundDisplay);
            TextView locationDisplay = findViewById(R.id.locationDisplay);
            locationDisplay.setText(audioLabel + "(" + db + "dB)");
            soundDisplay.setText(times[0] + ":" + times[1] + ", " + (int) Math.round(Double.parseDouble(confidence )*100) + "%");
            soundDisplay.setVisibility(View.VISIBLE);
            soundDisplay.setVisibility(View.VISIBLE);
            (findViewById(R.id.dontshowDisplay_layout)).setVisibility(View.VISIBLE);
            (findViewById(R.id.wearable_list_layout)).setVisibility(View.VISIBLE);
            TextView fTextView = (findViewById(R.id.dontshowDisplay));
            fTextView.setText("Don't notify this sound for:");
        }
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        Log.i(TAG, "Blocking sounds started");
        Integer tag = (Integer) v.itemView.getTag();

        if(tag!=wearableListCentralPosition)
            return;

        if(tag <= elementsInSec.length)     //Just an extra check
        {
            final int blockedNotificationID = ((MyApplication) getApplicationContext()).getIntegerValueOfSound(audioLabel);

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
                                soundDisplay.setText("SoundWatch app");
                                TextView fTextView = (findViewById(R.id.dontshowDisplay));
                                fTextView.setText("");
                            }
                        });

                        moveTaskToBack(true);
                    }
                }, 3000);
                return;
            }
            ((MyApplication) this.getApplication()).addBlockedSounds(blockedNotificationID);
            Log.i(TAG, "Add to list of blocked sounds");

            if (tag < elementsInSec.length)     //If not forever, start an alarm manager to remove sounds from blocked list. AlarmManager because Doze mode..
            {
                Intent alarmIntent = new Intent(this,  AlarmReceiver.class);
                alarmIntent.setAction("com.wearable.sound.almMgr");
                alarmIntent.putExtra("blockedSoundId", blockedNotificationID);
                alarmIntent.putExtra(SnoozeSoundService.SOUND_LABEL, audioLabel);
                alarmIntent.putExtra(SnoozeSoundService.CONNECTED_HOST_IDS, convertSetToCommaSeparatedList(connectedHostIds));
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

    /** Find the connected nodes that provide at least one of the given capabilities. */
    private void showNodes(final String... capabilityNames) {

        Task<Map<String, CapabilityInfo>> capabilitiesTask =
                Wearable.getCapabilityClient(this)
                        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE);

        capabilitiesTask.addOnSuccessListener(
                new OnSuccessListener<Map<String, CapabilityInfo>>() {
                    @Override
                    public void onSuccess(Map<String, CapabilityInfo> capabilityInfoMap) {
                        Set<Node> nodes = new HashSet<>();

                        if (capabilityInfoMap.isEmpty()) {
                            showDiscoveredNodes(nodes);
                            return;
                        }
                        for (String capabilityName : capabilityNames) {
                            CapabilityInfo capabilityInfo = capabilityInfoMap.get(capabilityName);
                            if (capabilityInfo != null) {
                                nodes.addAll(capabilityInfo.getNodes());
                            }
                        }
                        showDiscoveredNodes(nodes);
                    }
                });
    }

    private void showDiscoveredNodes(Set<Node> nodes) {
        List<String> nodesList = new ArrayList<>();
        for (Node node : nodes) {
            connectedHostIds.add(node.getId());
            nodesList.add(node.getDisplayName());
        }
        String msg;
        if (!nodesList.isEmpty()) {
            msg = getString(R.string.connected_nodes, TextUtils.join(", ", nodesList));
        } else {
            msg = getString(R.string.no_device);
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public class AudioLabel {
        String label;
        double confidence;
        String time;
        String db;
        String recordTime;
        public AudioLabel(String label, String confidence, String time, String db, String recordTime) {
            this.label = label;
            this.confidence = Double.parseDouble(confidence);
            this.time = time;
            this.db = db;
            this.recordTime = recordTime;
        }
    }

    public void createAudioLabelNotification(AudioLabel audioLabel) {

        final int NOTIFICATION_ID = ((MyApplication) getApplicationContext()).getIntegerValueOfSound(audioLabel.label);

        //If it's blocked or app in foreground (to not irritate user), return
        if ((((MyApplication) this.getApplication()).getBlockedSounds()).contains(NOTIFICATION_ID)
                || !((MyApplication) this.getApplication()).enabledSounds.contains(audioLabel.label)
                || ((MyApplication) this.getApplication()).isAppInForeground())
            return;

        Log.d(TAG, "generateBigTextStyleNotification()");
        if (!notificationChannelIsCreated) {
            createNotificationChannel();
            notificationChannelIsCreated = true;
        }

        Intent intent = new Intent(this, MainActivity.class);       //Just go the MainActivity for now. Replace with other activity if you want more actions.
        String[] dataPassed = {audioLabel.label, Double.toString(audioLabel.confidence), audioLabel.time, audioLabel.db};         //Adding data to be passed back to the main activity
        intent.putExtra("audio_label", dataPassed);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent snoozeIntent = new Intent(this, SnoozeSoundService.class);
        snoozeIntent.putExtra(SnoozeSoundService.SOUND_ID, NOTIFICATION_ID);
        snoozeIntent.putExtra(SnoozeSoundService.SOUND_LABEL, audioLabel.label);
        snoozeIntent.putExtra(SnoozeSoundService.CONNECTED_HOST_IDS, convertSetToCommaSeparatedList(connectedHostIds));
        //snoozeIntent.putExtra(SnoozeSoundService.SNOOZE_TIME, 10 * 60 * 1000);
        PendingIntent snoozeSoundPendingIntent = PendingIntent.getService(this, 0, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);


        String db = audioLabel.db;
        if (db.contains("\\.")) {
            String[] parts = db.split("\\.");
            db = parts[0];
        } else {
            db = db.substring(0, 2);
        }

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(audioLabel.label + ", " + (int) Math.round(audioLabel.confidence * 100) + "%")
                .setContentText("(" + db + " dB)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("")
//                        .bigText(audioLabel.label+ ", " + (int) Math.round(audioLabel.confidence * 100) + "%")
//                        .setBigContentTitle("Nearby Sound")
                        .setSummaryText(""))
                .setAutoCancel(true) //Remove notification from the list after the user has tapped it
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_full_cancel,
                        "10 min", snoozeSoundPendingIntent);

        //NOTIFICATION ID depends on the sound and the location so a particular sound in a particular location is only notified once until dismissed
        Log.d(TAG, "Notification Id: " + NOTIFICATION_ID);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notificationCompatBuilder.build());
        long startTime = 0;

        if(TEST_E2E_LATENCY) {
            long elapsedTime = System.currentTimeMillis() - Long.parseLong(audioLabel.recordTime);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String timeStamp = simpleDateFormat.format(date);

            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("e2e_latency" + ARCHITECTURE + ".txt", Context.MODE_APPEND));
                outputStreamWriter.write(timeStamp + "," +  Long.toString(elapsedTime) + "\n");
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }

    }
}
