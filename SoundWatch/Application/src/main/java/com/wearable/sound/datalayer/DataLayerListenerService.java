package com.wearable.sound.datalayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.health.SystemHealthManager;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;


import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.wearable.sound.R;
import com.wearable.sound.models.SoundPrediction;
import com.wearable.sound.ui.activity.MainActivity;
import com.wearable.sound.utils.Constants;

import static com.wearable.sound.ui.activity.MainActivity.AUDIO_LABEL;
import static com.wearable.sound.ui.activity.MainActivity.FOREGROUND_LABEL;
import static com.wearable.sound.ui.activity.MainActivity.PREDICT_MULTIPLE_SOUNDS;
import static com.wearable.sound.ui.activity.MainActivity.TEST_E2E_LATENCY;
import static com.wearable.sound.ui.activity.MainActivity.TEST_MODEL_LATENCY;


/** Listens to DataItems and Messages from the local node. */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "Phone/DataLayerService";
    private static final String UNIDENTIFIED_SOUND = "Unidentified Sound";

    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    private static final String AUDIO_PREDICTION_PATH = "/audio-prediction";
    private static final String SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH = "/SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH";
    private static final String CHANNEL_ID = "SOUNDWATCH";

    private static final float PREDICTION_THRES = 0.4F;
    private static double DBLEVEL_THRES = 40;
    private static final String SEND_CURRENT_BLOCKED_SOUND_PATH = "/SEND_CURRENT_BLOCKED_SOUND_PATH";
    private static final String WATCH_CONNECT_STATUS = "/WATCH_CONNECT_STATUS";

    public static final String COUNT_PATH = "/count";
    public static final String SOUND_SNOOZE_FROM_WATCH_PATH = "/SOUND_SNOOZE_FROM_WATCH_PATH";
    public static final String SOUND_UNSNOOZE_FROM_WATCH_PATH = "/SOUND_UNSNOOZE_FROM_WATCH_PATH";

    private Interpreter tfLite;
    //    private static final String MODEL_FILENAME = "file:///android_asset/example_model.tflite";
    private static final String MODEL_FILENAME = "file:///android_asset/sw_model_v2.tflite";
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";

    // must match with Wearable.SoundRecorder
    private static final int RECORDING_RATE = 16000; // (Hz == number of sample per second
    // note: vggish model require a buffer of minimum 5360 bytes to return a non-null result;
    //  that's equivalent to ~330ms of data with recording rate of 16kHz;
    //  for model v2, the buffer size is intended to be ~320ms
    private static final int bufferElements2Rec = 5360;
    private final List<String> labels = new ArrayList<>();
    private int numLabels;
    //    private double dbTotal = 0;
    private int counter = 0;

    private SharedPreferences.OnSharedPreferenceChangeListener autoUpdate;

    protected Python py;
    PyObject pythonModule;

    private static final int NUM_FRAMES = 32;  // Frames in input mel-spectrogram patch.
    private static final int NUM_BANDS = 64;// Frequency bands in input mel-spectrogram patch.
    private float [] input1D = new float [NUM_FRAMES * NUM_BANDS];
    //    private float [][][][] input4D = new float [1][96][64][1];
    private float [][][] input3D = new float [1][NUM_FRAMES][NUM_BANDS];
    private float[][] output;
    private long recordTime;


    private List<Short> soundBuffer = new ArrayList<>();
    private int soundSecondCounter = 0;

    /**
     * SocketIO part
     */
    private static final String SERVER_URL = "http://sheltered-dawn-06267.herokuapp.com/";

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://chat.socket.io");
        } catch (URISyntaxException e) {}
    }

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Architecture: " + MainActivity.ARCHITECTURE);
        Log.i(TAG, "Audio Transmission style: " + MainActivity.AUDIO_TRANMISSION_STYLE);

        // Load labels
        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
            br.close();
            this.numLabels = labels.size();
            this.output = new float[1][numLabels];
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file of " + actualLabelFilename, e);
        }

        // Load model
        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        DBLEVEL_THRES = sharedPref.getInt("db_threshold", 40);

        // Add slider for adjusting db threshold
        autoUpdate = mOnSharedPreferenceChangeListener;
        sharedPref.registerOnSharedPreferenceChangeListener(autoUpdate);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener
            mOnSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (key.equals("db_threshold")) {
            DBLEVEL_THRES = sharedPreferences.getInt(key, 40);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

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
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Message sent successfully");
                            } else {
                                Log.d(TAG, "Message failed.");
                            }
                        });
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "Message received from Watch");

        if (messageEvent.getPath().equals(SOUND_SNOOZE_FROM_WATCH_PATH)) {
            String soundLabel = (new String(messageEvent.getData())).split(",")[0];
            Log.i(TAG, "Phone received Snooze Sound [" + soundLabel + "] from watch");
            if (MainActivity.SOUNDS_MAP.containsKey(soundLabel)) {
                Log.i(TAG, "Setting is Snooze true");
                MainActivity.SOUNDS_MAP.get(soundLabel).isSnoozed = true;
                /** Display Snooze on Phone**/
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.mBroadcastSnoozeSound);
                broadcastIntent.putExtra(AUDIO_LABEL, soundLabel);
                sendBroadcast(broadcastIntent);
            }
            return;
        }

        if (messageEvent.getPath().equals(SOUND_UNSNOOZE_FROM_WATCH_PATH)) {
            String soundLabel = (new String(messageEvent.getData())).split(",")[0];
            Log.i(TAG, "Phone received UnSnooze Sound from watch: " + soundLabel);
            if (MainActivity.SOUNDS_MAP.containsKey(soundLabel)) {
                /** Display UnSnooze on Phone**/
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.mBroadcastUnsnoozeSound);
                broadcastIntent.putExtra(AUDIO_LABEL, soundLabel);
                sendBroadcast(broadcastIntent);
            }
            return;
        }

        if (messageEvent.getPath().equals(SEND_CURRENT_BLOCKED_SOUND_PATH)) {
            String blockedSoundsStr = new String(messageEvent.getData());
            Log.i(TAG, "Phone received Snoozed list on connected from watch: " + blockedSoundsStr);
            if (blockedSoundsStr != null) {
                String[] blockedSounds = blockedSoundsStr.split(",");
                for (String blockedSound : blockedSounds) {
                    if (MainActivity.SOUNDS_MAP.containsKey(blockedSound)) {
                        MainActivity.SOUNDS_MAP.get(blockedSound).isSnoozed = true;
                    }
                }
            }
            return;
        }

        // check if the watch is on and connected
        if (messageEvent.getPath().equals(WATCH_CONNECT_STATUS)) {
            String connectedStatus = new String(messageEvent.getData());
            Log.i(TAG, "Phone received Watch Status: " + connectedStatus);
            // check if the watch successfully start recording
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.mBroadcastForegroundService);
            broadcastIntent.putExtra(FOREGROUND_LABEL, connectedStatus);
            sendBroadcast(broadcastIntent);
            return;
        }

        /** Parsing data array from watch **/
        processAudioRecognition(messageEvent.getData());
    }

    public void processAudioRecognition(byte[] data) {
//        Log.i(TAG, "processAudioRcognition()");
        float[] features;
        short[] shorts;
        double db;
        byte[] dbData;
        byte[] featuresData;
        byte[] currentTimeData;
//        long recordTime;
        String prediction;
        switch (MainActivity.ARCHITECTURE) {
            case MainActivity.WATCH_ONLY_ARCHITECTURE:
            case MainActivity.WATCH_SERVER_ARCHITECTURE:
                Log.i(TAG, "Invalid architecture for phone");
                break;
            case MainActivity.PHONE_WATCH_ARCHITECTURE:
                switch (MainActivity.AUDIO_TRANMISSION_STYLE) {
                    case MainActivity.AUDIO_FEATURES_TRANSMISSION:
                        /** Predict sound with audio features **/
                        if (TEST_E2E_LATENCY) {
                            currentTimeData = new byte[Long.BYTES];
                            dbData = new byte[8];
                            featuresData = new byte[data.length - 8 - 8];
                            System.arraycopy(data, 0, currentTimeData, 0, Long.BYTES);
                            System.arraycopy(data, Long.BYTES, dbData, 0, 8);
                            System.arraycopy(data, Long.BYTES + 8, featuresData, 0, featuresData.length);
                            db = toDouble(dbData);
                            recordTime = bytesToLong(currentTimeData);
                            Log.i(TAG, "Record time received from watch: " + recordTime);
                            features = convertByteArrayToFloatArray(featuresData);
                            predictSoundsFromAudioFeatures(features, db, recordTime);
                        } else {
                            dbData = new byte[8];
                            featuresData = new byte[data.length - 8];
                            System.arraycopy(data, 0, dbData, 0, 8);
                            System.arraycopy(data, 8, featuresData, 0, featuresData.length);
                            db = toDouble(dbData);
                            Log.i(TAG, "Phone received loudness db: " + db);
                            features = convertByteArrayToFloatArray(featuresData);
                            predictSoundsFromAudioFeatures(features, db, null);
                        }
                        break;
                    case MainActivity.RAW_AUDIO_TRANSMISSION:
                        if (TEST_E2E_LATENCY) {
                            currentTimeData = new byte[Long.BYTES];
                            byte[] audioData = new byte[data.length - Long.BYTES];
                            System.arraycopy(data, 0, currentTimeData, 0, Long.BYTES);
                            System.arraycopy(data, Long.BYTES, audioData, 0, audioData.length);
                            recordTime = bytesToLong(currentTimeData);
//                            Log.i(TAG, "Record time received from watch: " + recordTime);
                            shorts = convertByteArrayToShortArray(data);
//                            if (soundBuffer.size() == 16000) {
//                                predictSoundsFromRawAudio();
//                            }
                            if (soundBuffer.size() <= bufferElements2Rec) {
                                for (short num : shorts) {
                                    // load up enough bufferElements2Rec samples, then predict
                                    if (soundBuffer.size() == bufferElements2Rec) {
                                        if (soundSecondCounter == 1) {
                                            // Skip ~330ms seconds to accommodate latency
                                            predictSoundsFromRawAudio();
                                            soundSecondCounter = 0;
                                        } else {
                                            soundSecondCounter++;
                                            soundBuffer = new ArrayList<>();
                                        }

                                    }
                                    soundBuffer.add(num);
                                }
                            }
                        } else {
                            //Log.i(TAG, "Buffer size: " + soundBuffer.size());
                            shorts = convertByteArrayToShortArray(data);
                            if (soundBuffer.size() == bufferElements2Rec) {
                                predictSoundsFromRawAudio();
                            }
                            if (soundBuffer.size() < bufferElements2Rec) {
                                for (short num : shorts) {
                                    if (soundBuffer.size() == bufferElements2Rec) {
                                        if (soundSecondCounter == 1) {
                                            // Skip 1 seconds to accommodate latency
                                            predictSoundsFromRawAudio();
                                            soundSecondCounter = 0;
                                        } else {
                                            soundSecondCounter++;
                                            soundBuffer = new ArrayList<>();
                                        }
                                    }
                                    soundBuffer.add(num);
                                }
                            }
                        }
                        break;
                    default:
                        Log.i(TAG, "Bad architecture");
                        break;
                }
                break;
            case MainActivity.PHONE_WATCH_SERVER_ARCHITECTURE:
                switch (MainActivity.AUDIO_TRANMISSION_STYLE) {
                    case MainActivity.AUDIO_FEATURES_TRANSMISSION:
                        /** Predict sound with audio features **/
                        if (TEST_E2E_LATENCY) {
                            currentTimeData = new byte[Long.BYTES];
                            dbData = new byte[8];
                            featuresData = new byte[data.length - 8 - 8];
                            System.arraycopy(data, 0, currentTimeData, 0, Long.BYTES);
                            System.arraycopy(data, Long.BYTES, dbData, 0, 8);
                            System.arraycopy(data, Long.BYTES + 8, featuresData, 0, featuresData.length);
                            db = toDouble(dbData);
                            recordTime = bytesToLong(currentTimeData);
                            Log.i(TAG, "Record time received from watch: " + recordTime);
                            features = convertByteArrayToFloatArray(featuresData);
                            sendSoundFeaturesToServer(features, db, recordTime);
                        } else {
                            dbData = new byte[8];
                            featuresData = new byte[data.length - 8];
                            System.arraycopy(data, 0, dbData, 0, 8);
                            System.arraycopy(data, 8, featuresData, 0, featuresData.length);
                            db = toDouble(dbData);
                            Log.i(TAG, "Phone received loudness db: " + db);
                            features = convertByteArrayToFloatArray(featuresData);
                            sendSoundFeaturesToServer(features, db, null);
                        }
                        break;
                    case MainActivity.RAW_AUDIO_TRANSMISSION:
                        if (TEST_E2E_LATENCY) {
                            currentTimeData = new byte[Long.BYTES];
                            byte[] audioData = new byte[data.length - Long.BYTES];
                            System.arraycopy(data, 0, currentTimeData, 0, Long.BYTES);
                            System.arraycopy(data, Long.BYTES, audioData, 0, audioData.length);
                            recordTime = bytesToLong(currentTimeData);
                            Log.i(TAG, "Record time received from watch: " + recordTime);
                            shorts = convertByteArrayToShortArray(data);
                            if (soundBuffer.size() == bufferElements2Rec) {
                                sendRawAudioToServer();
                            }
                            if (soundBuffer.size() < bufferElements2Rec) {
                                for (short num : shorts) {
                                    if (soundBuffer.size() == bufferElements2Rec) {
                                        sendRawAudioToServer();
                                    }
                                    soundBuffer.add(num);
                                }
                            }
                        } else {
                            shorts = convertByteArrayToShortArray(data);
                            if (soundBuffer.size() == bufferElements2Rec) {
                                sendRawAudioToServer();
                            }
                            if (soundBuffer.size() < bufferElements2Rec) {
                                for (short num : shorts) {
                                    if (soundBuffer.size() == bufferElements2Rec) {
                                        sendRawAudioToServer();
                                    }
                                    soundBuffer.add(num);
                                }
                            }
                        }
                        break;
                    default:
                        Log.i(TAG, "Bad Architecture");
                        break;
                }
                break;
            default:
                Log.i(TAG, "Invalid Architecture");
                break;
        }
    }

    /**
     * Audio Processing
     */
    private float[] extractAudioFeatures(List<Short> soundBuffer) {
//        float[] input1D = new float [6144];
        if (soundBuffer.size() != bufferElements2Rec) {
            // Sanity check, because sound has to be exactly bufferElements2Rec elements
            soundBuffer = new ArrayList<>();
            return null;
        }
        short[] sData = new short[bufferElements2Rec];
        for (int i = 0; i < soundBuffer.size(); i++) {
            sData[i] = soundBuffer.get(i);
        }
        try {
            if (db(sData) >= DBLEVEL_THRES && sData.length > 0) {
                // Lazily load python module to faster boot up processing
                if (py == null || pythonModule == null) {
                    synchronized (this) {
                        if (!Python.isStarted()) {
                            Python.start(new AndroidPlatform(this));
                        }
                    }

                    py = Python.getInstance();
                    pythonModule = py.getModule("main");
                }
                //Get MFCC features
                PyObject mfccFeatures = pythonModule.callAttr("audio_samples", Arrays.toString(sData));   //System.out.println("Sending to python: " + Arrays.toString(sData));

                //Parse features into a float array
                String inputString = mfccFeatures.toString();
                if (inputString.isEmpty()) {
                    return null;
                }
                inputString = inputString.replace("jarray('F')([", "").replace("])", "");
                String[] inputStringArr = inputString.split(", ");
                for (int i = 0; i < input1D.length; i++) {
                    if (inputStringArr[i].isEmpty()) {
                        return null;
                    }
                    input1D[i] = Float.parseFloat(inputStringArr[i]);
                }
                return input1D;
            }
            return null;
        } catch (PyException e) {
            Log.i(TAG, "Something went wrong parsing to MFCC feature");
            return null;
        }
    }

    private void sendSoundFeaturesToServer(float[] features, double db, Long recordTime) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", new JSONArray(features));
            jsonObject.put("db", Math.abs(db));
            jsonObject.put("time", "" + System.currentTimeMillis());
            if (TEST_E2E_LATENCY) {
                jsonObject.put("record_time", Long.toString(recordTime));
            }
            Log.i(TAG, "Data sent to server features from phone: "  + features.length + ", " + db);
            MainActivity.mSocket.emit("audio_feature_data", jsonObject);
        } catch (JSONException e) {
            Log.i(TAG, "Failed sending sound features to server");
            e.printStackTrace();
        }
    }

    private void sendRawAudioToServer() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", new JSONArray(soundBuffer));
            jsonObject.put("time", "" + System.currentTimeMillis());
            soundBuffer = new ArrayList<>();
            if (TEST_E2E_LATENCY) {
                jsonObject.put("record_time", recordTime);
            }
            Log.i(TAG, "Send raw audio to server");
            Log.i(TAG, "Connected: " + MainActivity.mSocket.connected());
            MainActivity.mSocket.emit("audio_data", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private short[] convertByteArrayToShortArray(byte[] bytes) {
//        Log.i(TAG, "convertByteArrayToShortArray()");
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    private float[] convertByteArrayToFloatArray(byte[] buffer) {
        try {
            ByteArrayInputStream bas = new ByteArrayInputStream(buffer);
            DataInputStream ds = new DataInputStream(bas);
            float[] fArr = new float[buffer.length / 4];  // 4 bytes per float
            for (int i = 0; i < fArr.length; i++) {
                fArr[i] = ds.readFloat();
            }
            return fArr;
        } catch (IOException e) {
            Log.i(TAG,"ERROR parsing bytes array to float array");
        }
        return null;
    }

    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    private double db(short[] data) {
        double rms = 0.0;
        int dataLength = 0;
        for (short datum : data) {
            if (datum != 0) {
                dataLength++;
            }
            rms += datum * datum;
        }
        rms = rms / dataLength;
        return 10 * Math.log10(rms);
    }

//    private double db(short[] data) {
//        double rms = 0.0;
//        int len = 0;
//        for (short datum : data) {
//            if (datum != 0) {
//                len++;
//            }
//            rms += Math.abs(datum);
//        }
//        rms = rms / len;
//        return 20 * Math.log10(rms);
//    }

//    public static double db(List<Short> soundBuffer) {
//        double rms = 0.0;
//        int dataLength = 0;
//        for (int i = 0; i < soundBuffer.size(); i++) {
//            if (soundBuffer.get(i) != 0) {
//                dataLength++;
//            }
//            rms += soundBuffer.get(i) * soundBuffer.get(i);
//        }
//        rms = rms / dataLength;
//        return 10 * Math.log10(rms);
//    }

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
        if (intent.getAction() == null || intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            // start service code
            String input = intent.getStringExtra("connectedHostIds");
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SoundWatch is working")
                    .setContentText("Press the Mic button on your watch to begin listening")
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);

        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            // stop service code
            stopForeground(true);
            stopSelfResult(1);
        }
        return START_NOT_STICKY;
    }


    /**
     *  Predicting sounds Functions on device
     *  ---- From Raw Audio
     *  ---- From features
     * **/

    private String predictSoundsFromAudioFeatures(float[] input1D, double db, Long recordTime) {
        Log.i(TAG, "Predicting sounds from audio features");
        // Resize to dimensions of model input
//        float [][][][] input4D = new float [1][96][64][1];
        int count = 0;
        for (int j = 0; j < NUM_FRAMES; j++) {
            for (int k = 0; k < NUM_BANDS; k++) {
                input3D[0][j][k] = input1D[count];
                count++;
            }
        }
        long startTime = 0;
        if(TEST_MODEL_LATENCY)
            startTime = System.currentTimeMillis();

        // Run inference
        tfLite.run(input3D, output);

        if(TEST_MODEL_LATENCY) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            Log.i(TAG, "Elasped time" + elapsedTime);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String timeStamp = simpleDateFormat.format(date);
            try {
                Log.i(TAG, "Writing time to a file");
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("watch_model.txt", Context.MODE_APPEND));
                outputStreamWriter.write(timeStamp + "," +  Long.toString(elapsedTime) + "\n");
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }

        // Find max and argmax
        float max = output[0][0];
        int argmax = 0;
        if (PREDICT_MULTIPLE_SOUNDS) {
            List<SoundPrediction> predictions = new ArrayList<>();
            for (int i = 0; i < numLabels; i++) {
                predictions.add(new SoundPrediction(labels.get(i), output[0][i]));
            }
            // Sort the predictions by value in decreasing order
            Collections.sort(predictions, Collections.reverseOrder());
            // Convert this map into a shape of sound=value_sound=value
            String result = "";
            for (SoundPrediction soundPrediction: predictions) {
                result += soundPrediction.getLabel() + "_" + soundPrediction.getAccuracy() + ",";
            }
            // Strip the last ","
            result = result.substring(0, result.length() - 1);
            new SendAllAudioPredictionsToWearTask(result, db, recordTime).execute();
            return result;
        }
        for (int i = 0; i < numLabels; i++) {
            if (max < output[0][i]) {
                max = output[0][i];
                argmax = i;
            }
        }

        if (max > PREDICTION_THRES) {
            //Get label and confidence
            final String prediction = labels.get(argmax);
            final String confidence = String.format("%,.2f", max);

            if (TEST_E2E_LATENCY) {
                new SendAudioLabelToWearTask(prediction, confidence, db, recordTime).execute();
            } else {
                new SendAudioLabelToWearTask(prediction, confidence, db, null).execute();
            }
            return prediction + ": " + (Double.parseDouble(confidence) * 100) + "%                           " + LocalTime.now();
        } else {
            if (TEST_E2E_LATENCY) {
                new SendAudioLabelToWearTask(UNIDENTIFIED_SOUND, "1.0", 0.0, recordTime).execute();
                return UNIDENTIFIED_SOUND + ": " + 1.0 + "%                           " + LocalTime.now();
            }
        }
        return "Unrecognized sound" + "                           " + LocalTime.now();
    }


    private String predictSoundsFromRawAudio() {
//        counter++;
        Log.d(TAG, "Counter: " + counter);
        if (soundBuffer.size() != bufferElements2Rec) {
            soundBuffer = new ArrayList<>();
            return "Invalid audio size";
        }

        // copy buffer to a new short array
        short[] sData = new short[bufferElements2Rec];
        for (int i = 0; i < soundBuffer.size(); i++) {
            sData[i] = soundBuffer.get(i);
        }
        soundBuffer = new ArrayList<>();
        System.out.println("sData length is " + sData.length);

        try {
            double decibel = db(sData);
            Log.d(TAG, "2. DB of data: " + decibel + "| DB_thresh: " + DBLEVEL_THRES);
            if (decibel >= DBLEVEL_THRES) {
                Log.d(TAG, "Within threshold.");
                //Log.i(TAG, "Time elapsed before Running chaquopy" + (System.currentTimeMillis() - recordTime));

                long startTimePython = System.currentTimeMillis();
                if (py == null || pythonModule == null) {
                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(this));
                    }

                    py = Python.getInstance();
                    pythonModule = py.getModule("main");
                    Log.i(TAG, "Time elapsed after init Python modules: " + (System.currentTimeMillis() - startTimePython));
                }

                // Get MFCC features
//                System.out.println("Sending to python:\n" + Arrays.toString(sData));
                System.out.println("=======sending to python array length of " + sData.length);
                PyObject mfccFeatures = pythonModule.callAttr("audio_samples", Arrays.toString(sData));

                Log.i(TAG, "Time elapsed after running Python " + (System.currentTimeMillis() - startTimePython));
//                System.out.println("=======mfccFeatures is " + mfccFeatures.toString());

                // Parse features into a float array
                String inputString = mfccFeatures.toString();
                inputString = inputString.replace("jarray('F')([", "").replace("])", "");
                String[] inputStringArr = inputString.split(", ");
                if (inputStringArr.length == 0) {
                    return "Empty MFCC feature";
                }
                System.out.println("=======inputStringArr length is " + inputStringArr.length);

                for (int i = 0; i < input1D.length; i++) {
                    if (inputStringArr[i].isEmpty()) {
                        return "Empty MFCC feature";
                    }
                    input1D[i] = Float.parseFloat(inputStringArr[i]);
                }

//                System.out.println("input1D is " + Arrays.toString(input1D));

                // Resize to dimensions of model input
                int count = 0;
                for (int j = 0; j < NUM_FRAMES; j++) {
                    for (int k = 0; k < NUM_BANDS; k++) {
                        input3D[0][j][k] = input1D[count];
                        count++;
                    }
                }

                long startTime = 0;
                if(TEST_MODEL_LATENCY)
                    startTime = System.currentTimeMillis();
                Log.i(TAG, "Elapsed time from watch to model on phone: " + (System.currentTimeMillis() - recordTime));

                // Run inference
                tfLite.run(input3D, output);

                System.out.println("=====output is " + Arrays.toString(output));

                if (PREDICT_MULTIPLE_SOUNDS) {
                    List<SoundPrediction> predictions = new ArrayList<>();
                    for (int i = 0; i < numLabels; i++) {
                        predictions.add(new SoundPrediction(labels.get(i), output[0][i]));
                    }
                    // Sort the predictions by value in decreasing order
                    Collections.sort(predictions, Collections.reverseOrder());

                    // Convert this map into a shape of sound=value_sound=value
                    StringBuilder result = new StringBuilder();
                    for (SoundPrediction soundPrediction: predictions) {
                        result.append(soundPrediction.getLabel()).append("_").append(soundPrediction.getAccuracy()).append(",");
                    }
                    // Strip the last ","
                    result = new StringBuilder(result.substring(0, result.length() - 1));

                    // TODO: Something with DB
                    new SendAllAudioPredictionsToWearTask(result.toString(), db(sData), recordTime).execute();
                    return result.toString();
                }


                if(TEST_MODEL_LATENCY) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    Log.i(TAG, "Elasped time" + elapsedTime);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
                    Date date = new Date(System.currentTimeMillis());
                    String timeStamp = simpleDateFormat.format(date);
                    try {
                        Log.i(TAG, "Writing time to a file");
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("watch_model.txt", Context.MODE_APPEND));
                        outputStreamWriter.write(timeStamp + "," +  Long.toString(elapsedTime) + "\n");
                        outputStreamWriter.close();
                    }
                    catch (IOException e) {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }
                }

                //Find max and argmax
                float max = output[0][0];
                int argmax = 0;
                for (int i = 0; i < numLabels; i++) {
                    if (max < output[0][i]) {
                        max = output[0][i];
                        argmax = i;
                    }
                }

                if (max > PREDICTION_THRES) {
                    //Get label and confidence
                    final String prediction = labels.get(argmax);
                    final String confidence = String.format("%,.2f", max);
                    // TODO: Something with DB
                    new SendAudioLabelToWearTask(prediction, confidence, Math.abs(db(sData)), recordTime).execute();
                    return prediction + ": " + (Double.parseDouble(confidence) * 100) + "%                           " + LocalTime.now();
                } else {
                    if (TEST_E2E_LATENCY) {
                        Log.i(TAG, "Audio < prethreshold");
                        new SendAudioLabelToWearTask(UNIDENTIFIED_SOUND, "1.0", 0.0, recordTime).execute();
                        return UNIDENTIFIED_SOUND + ": " + 1.0 + "%                           " + LocalTime.now();
                    }
                }
            }
        } catch (PyException e) {
            Log.i(TAG, "Something went wrong parsing to MFCC feature");
            return "Something went wrong parsing to MFCC feature";
        }

        if (TEST_E2E_LATENCY) {
            Log.i(TAG, "Audio < dbthreshold " + db(sData));
            new SendAudioLabelToWearTask(UNIDENTIFIED_SOUND, "1.0", 0.0, recordTime).execute();
        }
        Log.i(TAG, "Unrecognized Sound");
        return "Unrecognized sound" + "                           " + LocalTime.now();
    }

    public class SendAudioLabelToWearTask extends AsyncTask<Void, Void, Void> {
        private String prediction;
        private String confidence;
        private String db;
        private Long recordTime;

        public SendAudioLabelToWearTask(String prediction, String confidence, double db, Long recordTime) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.db = Double.toString(db);
            if (recordTime == null) {
                this.recordTime = null;
            } else {
                this.recordTime = recordTime;
            }
        }
        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            String result;
            if (TEST_E2E_LATENCY) {
                result = prediction + "," + confidence + "," + LocalTime.now() + "," + db + "," + recordTime;
            } else {
                result = prediction + "," + confidence + "," + LocalTime.now() + "," + db;
            }
            Log.i(TAG, "Number of connected devices: " + nodes.size());
            for (String node : nodes) {
                Log.i(TAG, "Sending sound prediction: " + result);
                sendMessageWithData(node, AUDIO_PREDICTION_PATH, result.getBytes());
            }
            return null;
        }
    }

    public class SendAllAudioPredictionsToWearTask extends AsyncTask<Void, Void, Void> {
        private String result;
        private String db;
        private Long recordTime;

        public SendAllAudioPredictionsToWearTask(String result, double db, Long recordTime) {
            this.db = Double.toString(db);
            this.result = result;
            if (recordTime == null) {
                this.recordTime = null;
            } else {
                this.recordTime = recordTime;
            }
        }

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            String result;
            if (TEST_E2E_LATENCY) {
                result = this.result + ";" + LocalTime.now() + ";" + db + ";" + recordTime;
            } else {
                result = this.result + ";" + LocalTime.now() + ";" + db;
            }
            Log.i(TAG, "Number of connected devices:" + nodes.size());
            for (String node : nodes) {
                Log.i(TAG, "Sending sound prediction: " + result);
                sendMessageWithData(node, SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH, result.getBytes());
            }
            return null;
        }
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
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
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
            Log.e(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

}
