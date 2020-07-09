/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wearable.sound.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.wearable.sound.datalayer.DataLayerListenerService;
import com.wearable.sound.R;
import com.wearable.sound.geofence.GeofenceActivity;
import com.wearable.sound.models.SoundNotification;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset to
 * the paired wearable.
 */
public class MainActivity extends Activity
            implements
            CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";

    public static final boolean TEST_MODEL_LATENCY = false;
    public static final boolean TEST_E2E_LATENCY = false;

    /**
     * Sound or sound features send configuration
     */
    public static final String RAW_AUDIO_TRANSMISSION = "RAW_AUDIO_TRANSMISSION";
    public static final String AUDIO_FEATURES_TRANSMISSION = "AUDIO_FEATURES_TRANSMISSION";
    public static final String AUDIO_TRANMISSION_STYLE = RAW_AUDIO_TRANSMISSION;

    /**
     * Phone Watch Architecture configuration ONLY!!!
     * [ EXPERIMENTAL ]
     */
    public static final boolean PREDICT_MULTIPLE_SOUNDS = true;

    /**
     * Architecture configurations
     */
    public static final String PHONE_WATCH_ARCHITECTURE = "PHONE_WATCH_ARCHITECTURE";
    public static final String PHONE_WATCH_SERVER_ARCHITECTURE = "PHONE_WATCH_SERVER_ARCHITECTURE";
    public static final String WATCH_ONLY_ARCHITECTURE = "WATCH_ONLY_ARCHITECTURE";
    public static final String WATCH_SERVER_ARCHITECTURE = "WATCH_SERVER_ARCHITECTURE";
    public static final String ARCHITECTURE = PHONE_WATCH_ARCHITECTURE;


    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String AUDIO_PREDICTION_PATH = "/audio-prediction";
    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";

    private boolean mCameraSupported = false;
    int BufferElements2Rec = 16000;

    private ListView mDataItemList;
    private Button mSendPhotoBtn;
    private ImageView mThumbView;
    private Bitmap mImageBitmap;
    private View mStartActivityBtn;
    private DataItemAdapter mDataItemListAdapter;

    // Send DataItem
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    //If recording from phone...
    //private static final int RECORDER_SAMPLERATE = 16000;
    //private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    //private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final float PREDICTION_THRES = 0.5F;
    private static final double DBLEVEL_THRES = -35.0;
    private Interpreter tfLite;
    private static final String MODEL_FILENAME = "file:///android_asset/example_model.tflite";
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";

    //TODO HUNG 5: This is a bad way of mapping. What if we decide not to display all these 30 sounds? Will we have to change this code manually? Can we figure out a way to specify just once what all sounds we want for each context (home, office, outdoors), and it gets synced on phone, watch and server?
    private static final String SPEECH = "Speech";
    private static final String KNOCKING = "Knocking";
    private static final String PHONE_RING = "Phone Ring";
    private static final String DOG_BARK = "Dog Bark";
    private static final String DRILL = "Drill";
    private static final String FIRE_SMOKE_ALARM = "Fire/Smoke Alarm";
    private static final String VACUUM = "Vacuum";
    private static final String BABY_CRY = "Baby Cry";
    private static final String CHOPPING = "Chopping";
    private static final String DOOR_IN_USE = "Door In Use";
    private static final String WATER_RUNNING = "Water Running";
    private static final String MICROWAVE= "Microwave";
    private static final String SHAVER= "Shaver";
    private static final String TOOTHBRUSH= "Toothbrush";
    private static final String BLENDER = "Blender";
    private static final String DISHWASHER = "Dishwasher";
    private static final String DOORBELL = "Doorbell";
    private static final String TOILET_FLUSH = "Toilet Flush";
    private static final String HAIR_DRYER= "Hair Dryer";
    private static final String LAUGHING= "Laughing";
    private static final String SNORING= "Snoring";
    private static final String HAMMERING= "Hammering";
    private static final String CAR_HONK= "Car Honk";
    private static final String VEHICLE= "Vehicle";
    private static final String SAW= "Saw";
    private static final String CAT_MEOW= "Cat Meow";
    private static final String ALARM_CLOCK= "Alarm Clock";
    private static final String UTENSILS_AND_CUTLERY= "Utensils and Cutlery";

    public List<String> sounds = Arrays.asList(UTENSILS_AND_CUTLERY, ALARM_CLOCK, CAT_MEOW, SAW, VEHICLE, CAR_HONK,
            HAMMERING, SNORING, LAUGHING, HAIR_DRYER, TOILET_FLUSH, DOORBELL, DISHWASHER, BLENDER, TOOTHBRUSH,DOG_BARK,
            MICROWAVE, WATER_RUNNING, DOOR_IN_USE, SHAVER, BABY_CRY, CHOPPING, VACUUM, DRILL, FIRE_SMOKE_ALARM, SPEECH,
            KNOCKING);

    private static final String SOUND_ENABLE_FROM_PHONE_PATH = "/SOUND_ENABLE_FROM_PHONE_PATH";
    public static final String AUDIO_LABEL = "AUDIO_LABEL";

    public static Map<String, SoundNotification> SOUNDS_MAP = new HashMap<String, SoundNotification>();

    {
        for (String sound : sounds) {
            SOUNDS_MAP.put(sound, new SoundNotification(sound, true, false));
        }
    }

    public void onCheckBoxClick(View view) {
        int id = view.getId();
        SoundNotification currentSound = null;
        switch (id) {
            case R.id.speech:
                currentSound = SOUNDS_MAP.get(SPEECH);
                break;
            case R.id.knocking:
                currentSound = SOUNDS_MAP.get(KNOCKING);
                break;
            case R.id.phone_ring:
                currentSound = SOUNDS_MAP.get(PHONE_RING);
                break;
            case R.id.vehicle:
                currentSound = SOUNDS_MAP.get(VEHICLE);
                break;
            case R.id.car_honk:
                currentSound = SOUNDS_MAP.get(CAR_HONK);
                break;
            case R.id.fire_smoke_alarm:
                currentSound = SOUNDS_MAP.get(FIRE_SMOKE_ALARM);
                break;
            case R.id.microwave:
                currentSound = SOUNDS_MAP.get(MICROWAVE);
                break;
            case R.id.water_running:
                currentSound = SOUNDS_MAP.get(WATER_RUNNING);
                break;
            case R.id.door_in_use:
                currentSound = SOUNDS_MAP.get(DOOR_IN_USE);
                break;
            case R.id.dishwasher:
                currentSound = SOUNDS_MAP.get(DISHWASHER);
                break;
            case R.id.laughing:
                currentSound = SOUNDS_MAP.get(LAUGHING);
                break;
            default:
                break;
        }
        if (currentSound != null) {
            boolean isEnabled = ((CheckBox) view).isChecked();
            CheckBox checkBox = ((CheckBox) view);
            if (checkBox.getText().toString().contains("Snoozed")) {
                // If the sound is currently snoozed
                int snoozeTextIndex = checkBox.getText().toString().indexOf("(Snoozed)");
                String soundLabel = checkBox.getText().toString().substring(0, snoozeTextIndex);
                checkBox.setText(soundLabel);
            }
            currentSound.isEnabled = isEnabled;
            new sendSoundEnableMessageToWatchTask(currentSound).execute();
        }
    }

    public class sendSoundEnableMessageToWatchTask extends AsyncTask<Void, Void, Void> {
        private String data;

        public sendSoundEnableMessageToWatchTask(SoundNotification soundNotification) {
            data = soundNotification.label + "," + soundNotification.isEnabled + "," + soundNotification.isSnoozed;
        }
        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            Log.i(TAG, "Sending enabled data to watch " + nodes.size());
            for (String node : nodes) {
                Log.i(TAG, "Sending enabled data from phone: " + data);
                sendMessageWithData(node, SOUND_ENABLE_FROM_PHONE_PATH, data.getBytes());
            }
            return null;
        }
    }


    private List<Short> soundBuffer = new ArrayList<>();

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

    private List<String> labels = new ArrayList<String>();

    int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    protected Python py;
    PyObject pythonModule;

    private float [] input1D = new float [6144];
    private float [][][][] input4D = new float [1][96][64][1];
    private float[][] output = new float[1][30];
    public static final String mBroadcastSoundPrediction = "com.wearable.sound.broadcast.soundprediction";
    public static final String mBroadcastSnoozeSound = "com.wearable.sound.broadcast.snoozeSound";
    public static final String mBroadcastUnsnoozeSound = "com.wearable.sound.broadcast.unsnoozeSound";

    private IntentFilter mIntentFilter;

    String[] permissions = new String[]{
            //Manifest.permission.INTERNET,
            //Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.VIBRATE,
            Manifest.permission.RECORD_AUDIO,
    };

    /**
     * SocketIO
     * @return
     */

    public static Socket mSocket;
    //    private static final String SERVER_URL = "http://128.208.49.41:8787";
    private static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
    private static final String MODEL_LATENCY_SERVER = "http://128.208.49.41:8790";
    private static final String DEFAULT_SERVER = "http://128.208.49.41:8788";

    {
        String SERVER_URL;
        if (TEST_E2E_LATENCY) {
            SERVER_URL = TEST_E2E_LATENCY_SERVER;
        } else if (TEST_MODEL_LATENCY) {
            SERVER_URL = MODEL_LATENCY_SERVER;
        } else {
            SERVER_URL = DEFAULT_SERVER;
        }
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
            String recordTime = "";
            try {
                audio_label = data.getString("label");
                accuracy = data.getString("accuracy");
                db = data.getString("db");
                if (TEST_E2E_LATENCY) {
                    recordTime = data.getString("record_time");
                }
            } catch (JSONException e) {
                Log.i(TAG, "JSON Exception failed: " + data.toString());
                return;
            }
            Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy + ", " + db);
            new SendAudioLabelToWearTask(audio_label, accuracy, db, recordTime).execute();
        }
    };

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
                return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG, "onCreate");

        //Ask permissions
        checkPermissions();
        if (ARCHITECTURE.equals(PHONE_WATCH_SERVER_ARCHITECTURE)) {
            mSocket.on("audio_label", onNewMessage);
            mSocket.connect();
        }

        //Initialize python module
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));

        }
        py = Python.getInstance();
        pythonModule = py.getModule("main");

        //Load labels
        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        //Load model
        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mCameraSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        setContentView(R.layout.main_activity);

        // Stores DataItems received by the local broadcaster or from the paired watch.
//        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
//        mDataItemList.setAdapter(mDataItemListAdapter);

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);



        Log.i(TAG, "Staring foreground service in main");
        Intent serviceIntent = new Intent(this, DataLayerListenerService.class);

        ContextCompat.startForegroundService(this, serviceIntent);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcastSoundPrediction);
        mIntentFilter.addAction(mBroadcastSnoozeSound);
        mIntentFilter.addAction(mBroadcastUnsnoozeSound);
        registerReceiver(mReceiver, mIntentFilter);
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
    public void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, DataLayerListenerService.class);
        stopService(serviceIntent);
        mSocket.disconnect();
        mSocket.off("audio_label", onNewMessage);
    }

    private double db(short[] data) {
        double rms = 0.0;
        for (int i = 0; i < data.length; i++) {
            rms += Math.abs(data[i]);
        }
        rms = rms/data.length;
        return 20 * Math.log10(rms/32768.0);
    }

    private short[] convertByteArrayToShortArray(byte[] bytes) {
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    private byte[] convertShortArrayToByteArray(short[] shorts) {
        byte[] result = new byte[shorts.length * 2];
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
        return result;
    }

    private String predictSounds() {
        if (soundBuffer.size() != 16000) {
            return "Invalid audio size";
        }
        short[] sData = new short[BufferElements2Rec];
        for (int i = 0; i < soundBuffer.size(); i++) {
            sData[i] = soundBuffer.get(i);
        }
        soundBuffer = new ArrayList<>();
        try {
            Log.i(TAG, "DB of data: " + db(sData));
            if (db(sData) >= DBLEVEL_THRES && sData.length > 0) {

                //Get MFCC features
                PyObject mfccFeatures = pythonModule.callAttr("audio_samples", Arrays.toString(sData));   //System.out.println("Sending to python: " + Arrays.toString(sData));

                //Parse features into a float array
                String inputString = mfccFeatures.toString();
                if (inputString.isEmpty()) {
                    return "Empty MFCC feature";
                }
                inputString = inputString.replace("jarray('F')([", "").replace("])", "");
                String[] inputStringArr = inputString.split(", ");
                for (int i = 0; i < 6144; i++) {
                    if (inputStringArr[i].isEmpty()) {
                        return "Empty MFCC feature";
                    }
                    input1D[i] = Float.parseFloat(inputStringArr[i]);
                }

                // Resize to dimensions of model input
                int count = 0;
                for (int j = 0; j < 96; j++) {
                    for (int k = 0; k < 64; k++) {
                        input4D[0][j][k][0] = input1D[count];
                        count++;
                    }
                }

                //Run inference
                tfLite.run(input4D, output);

                //Find max and argmax
                float max = output[0][0];
                int argmax = 0;
                for (int i = 0; i < 30; i++) {
                    if (max < output[0][i]) {
                        max = output[0][i];
                        argmax = i;
                    }
                }

                if (max > PREDICTION_THRES) {
                    //Get label and confidence
                    final String prediction = labels.get(argmax);
                    final String confidence = String.format("%,.2f", max);
                    new SendAudioLabelToWearTask(prediction, confidence, Double.toString(db(sData)), null).execute();
                    return prediction + ": " + (Double.parseDouble(confidence) * 100) + "%                           " + LocalTime.now();
                }
            }
        } catch (PyException e) {
            return "Something went wrong parsing to MFCC feature";
        }
        return "Unrecognized sound" + "                           " + LocalTime.now();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mBroadcastSoundPrediction)) {
                mDataItemListAdapter.add(new Event("Sound prediction", intent.getStringExtra("Sound prediction")));
            } else if (intent.getAction().equals(mBroadcastSnoozeSound)) {
                CheckBox checkBox = getCheckboxFromAudioLabel(intent.getStringExtra(AUDIO_LABEL));
                Log.i(TAG, "Getting checkbox main" + checkBox);
                SoundNotification soundNotification = SOUNDS_MAP.get(intent.getStringExtra(AUDIO_LABEL));
                soundNotification.isSnoozed = true;
                if (checkBox == null) {
                    return;
                }
                checkBox.setText(checkBox.getText() + " (Snoozed) ");
//                checkBox.setCompoundDrawablesWithIntrinsicBounds(0,0 , android.R.drawable.ic_lock_silent_mode, 0);
            } else if (intent.getAction().equals(mBroadcastUnsnoozeSound)) {
                Log.i(TAG, "Phone received unsnoozed");
                CheckBox checkBox = getCheckboxFromAudioLabel(intent.getStringExtra(AUDIO_LABEL));
                SoundNotification soundNotification = SOUNDS_MAP.get(intent.getStringExtra(AUDIO_LABEL));
                soundNotification.isSnoozed = false;
                if (checkBox == null) {
                    return;
                }
                if (checkBox.getText().toString().contains("Snoozed")) {
                    // If the sound is currently snoozed
                    int snoozeTextIndex = checkBox.getText().toString().indexOf("(Snoozed)");
                    String soundLabel = checkBox.getText().toString().substring(0, snoozeTextIndex);
                    checkBox.setText(soundLabel);
                }
            }
        }
    };

    public CheckBox getCheckboxFromAudioLabel(String audioLabel) {
        switch (audioLabel) {
            case SPEECH:
                return (CheckBox) findViewById(R.id.speech);
            case KNOCKING:
                return (CheckBox) findViewById(R.id.knocking);
            case PHONE_RING:
                return (CheckBox) findViewById(R.id.phone_ring);
            case UTENSILS_AND_CUTLERY:
                return (CheckBox) findViewById(R.id.knocking);
            case CHOPPING:
                return (CheckBox) findViewById(R.id.knocking);
            case VEHICLE:
                return (CheckBox) findViewById(R.id.vehicle);
            case CAR_HONK:
                return (CheckBox) findViewById(R.id.car_honk);
            case FIRE_SMOKE_ALARM:
                return (CheckBox) findViewById(R.id.fire_smoke_alarm);
            case MICROWAVE:
                return (CheckBox) findViewById(R.id.microwave);
            case WATER_RUNNING:
                return (CheckBox) findViewById(R.id.water_running);
            case DOOR_IN_USE:
                return (CheckBox) findViewById(R.id.door_in_use);
            case DISHWASHER:
                return (CheckBox) findViewById(R.id.dishwasher);
            case LAUGHING:
                return (CheckBox) findViewById(R.id.laughing);
            default:
                return null;
        }
    }

        @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            mImageBitmap = (Bitmap) extras.get("data");
            mThumbView.setImageBitmap(mImageBitmap);
        }
    }


    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        LOGD(TAG, "onCapabilityChanged: " + capabilityInfo);

        mDataItemListAdapter.add(new Event("onCapabilityChanged", capabilityInfo.toString()));
    }

    /** Sends an RPC to start a fullscreen Activity on the wearable. */
    public void onStartWearableActivityClick(View view) {
        LOGD(TAG, "Generating RPC");
        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();
    }
    public void onGeofenceClick(View view) {
        Log.d(TAG, "Navigating to GeofenceActivity");
        Intent intent = new Intent(this, GeofenceActivity.class);
        startActivity(intent);
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
            LOGD(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    @WorkerThread
    private void sendStartActivityMessage(String node) {

        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this).sendMessage(node, START_ACTIVITY_PATH, new byte[0]);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            LOGD(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    /**
     * Dispatches an {@link Intent} to take a photo. Result will be returned back in
     * onActivityResult().
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Builds an {@link Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created from the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);

        dataItemTask.addOnSuccessListener(
                new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        LOGD(TAG, "Sending image was successful: " + dataItem);
                    }
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
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }

    /** As simple wrapper around Log.d */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    /** A View Adapter for presenting the Event objects in a list */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    public class SendAudioLabelToWearTask extends AsyncTask<Void, Void, Void> {
        private String prediction;
        private String confidence;
        private String db;
        private String recordTime;

        public SendAudioLabelToWearTask(String prediction, String confidence, String db, String recordTime) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.recordTime = recordTime;
            this.db = db;
        }
        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            String result = prediction + "," + confidence + "," + LocalTime.now() + "," + db + "," + recordTime;
            for (String node : nodes) {
                Log.i(TAG, "Sending sound prediction: " + result);
                sendMessageWithData(node, AUDIO_PREDICTION_PATH, result.getBytes());
            }
            return null;
        }
    }

    /** Generates a DataItem based on an incrementing count. */
    private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            LOGD(TAG, "Generating DataItem: " + request);

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItem dataItem = Tasks.await(dataItemTask);

                LOGD(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }
}
