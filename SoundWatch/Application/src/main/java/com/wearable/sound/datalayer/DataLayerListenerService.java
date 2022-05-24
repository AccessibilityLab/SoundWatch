package com.wearable.sound.datalayer;

import static com.wearable.sound.ui.activity.MainActivity.AUDIO_LABEL;
import static com.wearable.sound.ui.activity.MainActivity.FOREGROUND_LABEL;
import static com.wearable.sound.ui.activity.MainActivity.TEST_E2E_LATENCY;
import static com.wearable.sound.utils.Constants.ACTION;
import static com.wearable.sound.utils.Constants.ARCHITECTURE;
import static com.wearable.sound.utils.Constants.AUDIO_FEATURES_TRANSMISSION;
import static com.wearable.sound.utils.Constants.AUDIO_TRANSMISSION_STYLE;
import static com.wearable.sound.utils.Constants.DBLEVEL_PARAM;
import static com.wearable.sound.utils.Constants.DEBUG_LOG;
import static com.wearable.sound.utils.Constants.PHONE_WATCH_ARCHITECTURE;
import static com.wearable.sound.utils.Constants.PHONE_WATCH_SERVER_ARCHITECTURE;
import static com.wearable.sound.utils.Constants.PREDICTION_EVENT;
import static com.wearable.sound.utils.Constants.PREDICT_MULTIPLE_SOUNDS;
import static com.wearable.sound.utils.Constants.RAW_AUDIO_TRANSMISSION;
import static com.wearable.sound.utils.Constants.SOUND_PREDICTION_MESSAGE;
import static com.wearable.sound.utils.Constants.WATCH_ONLY_ARCHITECTURE;
import static com.wearable.sound.utils.Constants.WATCH_SERVER_ARCHITECTURE;
import static com.wearable.sound.utils.HelperUtils.bytesToLong;
import static com.wearable.sound.utils.HelperUtils.convertByteArrayToShortArray;
import static com.wearable.sound.utils.HelperUtils.db;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.wearable.sound.R;
import com.wearable.sound.models.SoundPrediction;
import com.wearable.sound.ui.activity.MainActivity;
import com.wearable.sound.utils.AudioProcessors;
import com.wearable.sound.utils.Logger;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens to DataItems and Messages from the local node.
 * Make predictions based on the audio data.
 */
public class DataLayerListenerService extends WearableListenerService {
    public static final String TAG = "Phone/DataLayerService";
//    private static final String UNIDENTIFIED_SOUND = "Unidentified Sound";

    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    private static final String AUDIO_PREDICTION_PATH = "/audio-prediction";
    private static final String SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH = "/SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH";
    private static final String CHANNEL_ID = "SOUNDWATCH";

    public static double DBLEVEL_THRES = 40;

    public static final String SEND_CURRENT_BLOCKED_SOUND_PATH = "/SEND_CURRENT_BLOCKED_SOUND_PATH";
    public static final String WATCH_CONNECT_STATUS = "/WATCH_CONNECT_STATUS";
    public static final String COUNT_PATH = "/count";
    public static final String SOUND_SNOOZE_FROM_WATCH_PATH = "/SOUND_SNOOZE_FROM_WATCH_PATH";
    public static final String SOUND_UNSNOOZE_FROM_WATCH_PATH = "/SOUND_UNSNOOZE_FROM_WATCH_PATH";

    public static final String AUDIO_MESSAGE_PATH = "/audio_message";

    // Separating predicting logic into the class AudioProcessors
    private AudioProcessors audioProcessors;

    /**
     * Instead of deprecated AsyncTask API, use java.util.concurrent to run async tasks instead
     * https://stackoverflow.com/questions/58767733/android-asynctask-api-deprecating-in-android-11-what-are-the-alternatives
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Firebase
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG_LOG) {
            Log.i(TAG, "Architecture: " + ARCHITECTURE);
            Log.i(TAG, "Audio Transmission style: " + AUDIO_TRANSMISSION_STYLE);
        }

        // [FS-Logging] Log the when this first started up?
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        this.audioProcessors = new AudioProcessors(this);

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        DBLEVEL_THRES = sharedPref.getInt("db_threshold", 40);

        // Add slider for adjusting db threshold
        sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals("db_threshold")) {
                DBLEVEL_THRES = sharedPreferences.getInt(key, 40);

                // [FS-Logging] Log that the user change the threshold
                Bundle bundle = new Bundle();
                bundle.putInt(DBLEVEL_PARAM, sharedPreferences.getInt(key, 40));
                this.fsLogging("threshold_change", bundle,
                        ZonedDateTime.now().toString());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.fsLogging("on_destroy", new Bundle(),
                ZonedDateTime.now().toString());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (DEBUG_LOG) Log.d(TAG, "onDataChanged: " + dataEvents);

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();

            if (COUNT_PATH.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                // Send the rpc
                // Instantiates clients without member variables, as clients are inexpensive to
                // create. (They are cached and shared between GoogleApi instances.)
                Task<Integer> sendMessageTask =
                        Wearable.getMessageClient(this)
                                .sendMessage(nodeId, DATA_ITEM_RECEIVED_PATH, payload);

                sendMessageTask.addOnCompleteListener(
                        task -> {
                            if (DEBUG_LOG) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Message sent successfully");
                                } else {
                                    Log.d(TAG, "Message failed.");
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (DEBUG_LOG) Log.i(TAG, "Message received from Watch");

        // [FS-Logging]: Receive message from watch
//        fsLogging("message_received_from_watch", new Bundle());

        if (messageEvent.getPath().equals(SOUND_SNOOZE_FROM_WATCH_PATH)) {
            String soundLabel = (new String(messageEvent.getData())).split(",")[0];
            if (DEBUG_LOG)
                Log.i(TAG, "Phone received Snooze Sound [" + soundLabel + "] from watch");
            if (MainActivity.SOUNDS_MAP.containsKey(soundLabel)) {
                if (DEBUG_LOG) Log.i(TAG, "Setting is Snooze true");
                Objects.requireNonNull(MainActivity.SOUNDS_MAP.get(soundLabel)).isSnoozed = true;
                // Display Snooze on Phone
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.mBroadcastSnoozeSound);
                broadcastIntent.putExtra(AUDIO_LABEL, soundLabel);
                sendBroadcast(broadcastIntent);
            }
            return;
        }

        if (messageEvent.getPath().equals(SOUND_UNSNOOZE_FROM_WATCH_PATH)) {
            String soundLabel = (new String(messageEvent.getData())).split(",")[0];
            if (DEBUG_LOG) Log.i(TAG, "Phone received UnSnooze Sound from watch: " + soundLabel);
            if (MainActivity.SOUNDS_MAP.containsKey(soundLabel)) {
                // Display UnSnooze on Phone*
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.mBroadcastUnsnoozeSound);
                broadcastIntent.putExtra(AUDIO_LABEL, soundLabel);
                sendBroadcast(broadcastIntent);
            }
            return;
        }

        if (messageEvent.getPath().equals(SEND_CURRENT_BLOCKED_SOUND_PATH)) {
            String blockedSoundsStr = new String(messageEvent.getData());
            if (DEBUG_LOG)
                Log.i(TAG, "Phone received Snoozed list on connected from watch: " + blockedSoundsStr);
            String[] blockedSounds = blockedSoundsStr.split(",");
            for (String blockedSound : blockedSounds) {
                if (MainActivity.SOUNDS_MAP.containsKey(blockedSound)) {
                    Objects.requireNonNull(MainActivity.SOUNDS_MAP.get(blockedSound)).isSnoozed = true;
                }
            }
            return;
        }

        // check if the watch is on and connected
        if (messageEvent.getPath().equals(WATCH_CONNECT_STATUS)) {
            String connectedStatus = new String(messageEvent.getData());
            if (DEBUG_LOG) Log.i(TAG, "Phone received Watch Status: " + connectedStatus);
            // check if the watch successfully start recording
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.mBroadcastForegroundService);
            broadcastIntent.putExtra(FOREGROUND_LABEL, connectedStatus);
            sendBroadcast(broadcastIntent);
            return;
        }

        // Else, getting audio data from watch --> parsing data array from watch
        if (messageEvent.getPath().equals(AUDIO_MESSAGE_PATH)) {
            processAudioRecognition(messageEvent.getData());
        }
    }

    private void processAudioRecognition(byte[] data) {
        if (DEBUG_LOG) Log.i(TAG, "Processing audio recognition...");
        // [FS-Logging]
//        fsLogging("start_processing_audio", new Bundle());

        switch (ARCHITECTURE) {
            case WATCH_ONLY_ARCHITECTURE:
            case WATCH_SERVER_ARCHITECTURE:
                if (DEBUG_LOG) Log.i(TAG, "Invalid architecture for phone");
                break;
            case PHONE_WATCH_ARCHITECTURE:
                switch (AUDIO_TRANSMISSION_STYLE) {
                    case AUDIO_FEATURES_TRANSMISSION:
                        // Predict sound with audio features
                        // TODO: implement this later; for now, to reduce wearable app size,
                        //  we only send raw audio data directly to phone
                        break;
                    case RAW_AUDIO_TRANSMISSION:
                        handleRawAudioTransmission(data);
                    default:
                        if (DEBUG_LOG) Log.i(TAG, "Invalid transmission architecture");
                        break;
                }
                break;
            case PHONE_WATCH_SERVER_ARCHITECTURE:
                if (DEBUG_LOG) Log.i(TAG, "Server Not Used For Now");
                break;
            default:
                if (DEBUG_LOG) Log.i(TAG, "Invalid Phone/Watch/Server Architecture");
                break;
        }
    }

    /**
     * Process the raw byte of data and return the prediction string
     *
     * @param data
     * @return
     */
    private void handleRawAudioTransmission(byte[] data) {
        // the raw audio sent to phone is in this format:
        //  [ --- record timestamp ---, --------- audio data --------- ]
        byte[] timestampData = new byte[Long.BYTES];
        byte[] audioData = new byte[data.length - Long.BYTES];
        System.arraycopy(data, 0, timestampData, 0, Long.BYTES);
        System.arraycopy(data, Long.BYTES, audioData, 0, audioData.length);
        long recordTimestamp = bytesToLong(timestampData);
        short[] sData = convertByteArrayToShortArray(audioData);

        if (DEBUG_LOG)
            Log.d(TAG, "Handling raw audio transmission: record timestamp is " + recordTimestamp +
                    "; the array of audio shorts has a length of " + sData.length);

        List<SoundPrediction> predictionList = this.audioProcessors.predictSoundFromRawAudio(sData, PREDICT_MULTIPLE_SOUNDS);
        if (predictionList == null || predictionList.isEmpty()) {
            // something went wrong
            if (DEBUG_LOG) Log.i(TAG, "No predictions are made");
            return;
        }

        if (PREDICT_MULTIPLE_SOUNDS) {
            // Convert this map into a shape of sound=value_sound=value, only include sounds with
            //  accuracy >= threshold
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (SoundPrediction soundPrediction : predictionList) {
                // Only include the label in available labels
                sb.append(separator);
                sb.append(soundPrediction.toString());
                separator = ",";
            }

            if (sb.length() > 0) {
                String predictionString = sb.toString();
                if (DEBUG_LOG)
                    Log.i(TAG, "Prediction string before sending is " + predictionString);
                sendAllAudioPredictionsToWearTask(sb.toString(), db(sData), recordTimestamp);
            }
        } else {
            SoundPrediction soundPrediction = predictionList.get(0);
            sendAudioLabelToWearTask(soundPrediction.getLabel(), soundPrediction.getLabel(), db(sData), recordTimestamp);
        }
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == null || intent.getAction().equals(ACTION.START_FOREGROUND_ACTION)) {
            if (DEBUG_LOG) Log.i(TAG, "Received Start Foreground Intent ");
            // start service code
            String input = intent.getStringExtra("connectedHostIds");
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SoundWatch is working")
                    .setContentText("Press the Mic button on your watch to begin listening")
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);

        } else if (intent.getAction().equals(ACTION.STOP_FOREGROUND_ACTION)) {
            if (DEBUG_LOG) Log.i(TAG, "Received Stop Foreground Intent");
            // stop service code
            stopForeground(true);
            stopSelfResult(1);
        }
        return START_NOT_STICKY;
    }

    private void sendAllAudioPredictionsToWearTask(String result, double db, Long recordTime) {
        executorService.execute(() -> {
            // background work here
            Collection<String> nodes = getNodes();
            String message;
            if (TEST_E2E_LATENCY) {
                message = result + ";" + ZonedDateTime.now() + ";" + db + ";" + recordTime;
            } else {
                message = result + ";" + ZonedDateTime.now() + ";" + db;
            }
            if (DEBUG_LOG) Log.i(TAG, "Number of connected devices:" + nodes.size());

            for (String node : nodes) {
                if (DEBUG_LOG) Log.i(TAG, "Sending sound prediction: " + message);
                sendMessageWithData(node, SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH, message.getBytes());
            }

            // [FS-Logging]
            Bundle bundle = new Bundle();
            bundle.putString(SOUND_PREDICTION_MESSAGE, message);
            this.fsLogging(PREDICTION_EVENT, bundle,
                    ZonedDateTime.now().toString());
        });
    }

    private void sendAudioLabelToWearTask(String prediction, String confidence, double db, Long recordTime) {
        executorService.execute(() -> {
            // background work here
            Collection<String> nodes = getNodes();
            String message;
            if (TEST_E2E_LATENCY) {
                message = prediction + "," + confidence + "," + ZonedDateTime.now() + "," + db + "," + recordTime;
            } else {
                message = prediction + "," + confidence + "," + ZonedDateTime.now() + "," + db;
            }
            if (DEBUG_LOG) Log.i(TAG, "Number of connected devices: " + nodes.size());

            for (String node : nodes) {
                if (DEBUG_LOG) Log.i(TAG, "Sending sound prediction: " + message);
                sendMessageWithData(node, AUDIO_PREDICTION_PATH, message.getBytes());
            }

            // [FS-Logging]
            Bundle bundle = new Bundle();
            bundle.putString(SOUND_PREDICTION_MESSAGE, message);
            if (DEBUG_LOG) Log.i(TAG, "log with firebase: message is " + message);
            this.fsLogging(PREDICTION_EVENT, bundle,
                    ZonedDateTime.now().toString());
        });
    }

    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                results.add(node.getId());
            }

        } catch (ExecutionException exception) {
            if (DEBUG_LOG) Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            if (DEBUG_LOG) Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }

    @WorkerThread
    private void sendMessageWithData(String node, String title, byte[] data) {
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this)
                        .sendMessage(node, title, data);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            if (DEBUG_LOG) Log.e(TAG, "Message sent: " + result);
        } catch (ExecutionException exception) {
            if (DEBUG_LOG) Log.e(TAG, "Task failed: " + exception);
        } catch (InterruptedException exception) {
            if (DEBUG_LOG) Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    private void fsLogging(String eventName, Bundle bundle, String ts) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Logger.fsLogging(this, this.mFirebaseAnalytics, TAG, eventName, bundle, ts);
        });
    }
}
