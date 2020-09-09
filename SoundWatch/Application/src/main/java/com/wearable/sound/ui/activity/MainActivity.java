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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wearable.sound.datalayer.DataLayerListenerService;
import com.wearable.sound.R;
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
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
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
public class MainActivity extends AppCompatActivity
            implements
            CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";

    /**
     * Different Mode configuration
     */
    public static final int HIGH_ACCURACY_MODE = 1;

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

    private ConstraintLayout tutorialLayout;
    private ListView mDataItemList;
    private Button mSendPhotoBtn;
    private Button tutorialBtn;
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
    private static final double DBLEVEL_THRES = 45.0;
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
    private static final String COUGHING = "Coughing";
    private static final String TYPING = "Typing";

    // List of all sounds
    public List<String> sounds = Arrays.asList(UTENSILS_AND_CUTLERY, ALARM_CLOCK, CAT_MEOW, SAW, VEHICLE, CAR_HONK,
            HAMMERING, SNORING, LAUGHING, HAIR_DRYER, TOILET_FLUSH, DOORBELL, DISHWASHER, BLENDER, TOOTHBRUSH,DOG_BARK,
            MICROWAVE, WATER_RUNNING, DOOR_IN_USE, SHAVER, BABY_CRY, CHOPPING, VACUUM, DRILL, FIRE_SMOKE_ALARM, SPEECH,
            KNOCKING, COUGHING, TYPING);

    // List of IDs of only high accuracy sounds (11 sounds)
    private static List<Integer> highAccuracyList = new ArrayList<>(Arrays.asList(R.id.fire_smoke_alarm, R.id.speech,
            R.id.door_in_use, R.id.water_running, R.id.knocking, R.id.microwave, R.id.dog_bark, R.id.cat_meow, R.id.car_honk,
            R.id.vehicle, R.id.baby_crying));
    // List of IDs of only low accuracy sounds (19 sounds)
    private static List<Integer> lowAccuracyList = new ArrayList<>(Arrays.asList(R.id.utensils_and_cutlery, R.id.alarm_clock,
            R.id.saw, R.id.hammering, R.id.snoring, R.id.laughing, R.id.hair_dryer, R.id.toilet_flush, R.id.door_bell,
            R.id.dishwasher, R.id.blender, R.id.tooth_brush, R.id.shaver, R.id.chopping, R.id.vacuum, R.id.drill, R.id.speech,
            R.id.coughing, R.id.typing));

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
            case R.id.door_bell:
                currentSound = SOUNDS_MAP.get(DOORBELL);
                break;
            case R.id.shaver:
                currentSound = SOUNDS_MAP.get(SHAVER);
                break;
            case R.id.tooth_brush:
                currentSound = SOUNDS_MAP.get(TOOTHBRUSH);
                break;
            case R.id.toilet_flush:
                currentSound = SOUNDS_MAP.get(VACUUM);
                break;
            case R.id.baby_crying:
                currentSound = SOUNDS_MAP.get(BABY_CRY);
                break;
            case R.id.chopping:
                currentSound = SOUNDS_MAP.get(CHOPPING);
                break;
            case R.id.blender:
                currentSound = SOUNDS_MAP.get(BLENDER);
                break;
            case R.id.hair_dryer:
                currentSound = SOUNDS_MAP.get(HAIR_DRYER);
                break;
            case R.id.snoring:
                currentSound = SOUNDS_MAP.get(SNORING);
                break;
            case R.id.hammering:
                currentSound = SOUNDS_MAP.get(HAMMERING);
                break;
            case R.id.saw:
                currentSound = SOUNDS_MAP.get(SAW);
                break;
            case R.id.cat_meow:
                currentSound = SOUNDS_MAP.get(CAT_MEOW);
                break;
            case R.id.alarm_clock:
                currentSound = SOUNDS_MAP.get(ALARM_CLOCK);
                break;
            case R.id.utensils_and_cutlery:
                currentSound = SOUNDS_MAP.get(UTENSILS_AND_CUTLERY);
                break;
            case R.id.dog_bark:
                currentSound = SOUNDS_MAP.get(DOG_BARK);
                break;
            case R.id.drill:
                currentSound = SOUNDS_MAP.get(DRILL);
                break;
            case R.id.vacuum:
                currentSound = SOUNDS_MAP.get(VACUUM);
                break;
            case R.id.laughing:
                currentSound = SOUNDS_MAP.get(LAUGHING);
                break;
            case R.id.coughing:
                currentSound = SOUNDS_MAP.get(COUGHING);
                break;
            case R.id.typing:
                currentSound = SOUNDS_MAP.get(TYPING);
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
//                checkBox.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            currentSound.isEnabled = isEnabled;
            new sendSoundEnableMessageToWatchTask(currentSound).execute();
        }
    }

    public void onTutorialClick(View view) {
        Log.d(TAG, "onTutorialClick called");
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
    private static ByteBuffer loadModelFile(AssetManager assets, String modelFilename)
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
//        try {
//            Context context = createPackageContext("com.wearable.sound", 0);
//            AssetManager assetManager = context.getAssets();
//            tfLite = new Interpreter(loadModelFile(assetManager, actualModelFilename));
//        } catch (PackageManager.NameNotFoundException | IOException e) {
//            e.printStackTrace();
//        }
        try {
            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mCameraSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        setContentView(R.layout.activity_main);

//        RelativeLayout parent = findViewById(R.id.main_layout);

        // inflate help_fragment into main_layout

//        LayoutInflater inflater =
//                (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View convertView = inflater.inflate(R.layout.help_fragment, parent);
//        tutorialLayout = convertView.findViewById(R.id.tutorial_layout);
//        tutorialLayout.setVisibility(View.GONE);
//        tutorialBtn = convertView.findViewById(R.id.tutorial_btn);
//        tutorialBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "onClick called");
//                Intent tutorial = new Intent(MainActivity.this, Tutorial.class);
//                startActivity(tutorial);
//            }
//        });

        // create BottomNavigationView
        LOGD(TAG, "create BottomNavigationView");
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        bottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomNavView.setElevation(32);
        // bottomNavView.setSelectedItemId(R.id.bottom_navigation_item_sound_list);

        // set Mode
        setAccuracyMode(1);

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

    private final BottomNavigationView.OnNavigationItemSelectedListener
            mOnNavigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    TextView titleView = findViewById(R.id.title_text);
                    ScrollView scrollView = findViewById(R.id.scroll_view);
                    FrameLayout frameLayout = findViewById(R.id.fragment_container);
                    TextView instructionalView = findViewById(R.id.instruction_text);
                    Fragment fragment = null;
                    switch (item.getItemId()) {
                        case R.id.bottom_navigation_item_home:
                            titleView.setText(R.string.soundwatch);
                            fragment = null;
                            frameLayout.setVisibility(View.GONE);
                            instructionalView.setVisibility(View.VISIBLE);
                            scrollView.setVisibility(View.VISIBLE);
//                            tutorialLayout.setVisibility(View.GONE);
                            break;
                        case R.id.bottom_navigation_item_about:
                            titleView.setText(R.string.about);
                            fragment = new ScrollingFragment();
                            frameLayout.setVisibility(View.VISIBLE);
                            instructionalView.setVisibility(View.GONE);
                            scrollView.setVisibility(View.GONE);
//                            tutorialLayout.setVisibility(View.GONE);
                            break;
                        case R.id.bottom_navigation_item_help:
                            titleView.setText(R.string.help);
                            fragment = new HelpFragment();
                            frameLayout.setVisibility(View.VISIBLE);
                            instructionalView.setVisibility(View.GONE);
                            scrollView.setVisibility(View.GONE);
//                            tutorialLayout.setVisibility(View.VISIBLE);

//                            Intent tutorial = new Intent(MainActivity.this, Tutorial.class);
//                            startActivity(tutorial);
                            break;
                        default:
                            break;
                    }
                    loadFragment(fragment);
                    return true;
                }
            };
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
                if (soundNotification != null) {
                    soundNotification.isSnoozed = true;
                }
                if (checkBox == null) {
                    return;
                }
                if (!checkBox.getText().toString().contains("Snoozed")) {
                    checkBox.setText(MessageFormat.format("{0} (Snoozed) ", checkBox.getText()));
                }
                checkBox.setChecked(false);
//                checkBox.setCompoundDrawablesWithIntrinsicBounds(0,0 , android.R.drawable.ic_lock_silent_mode, 0);
            } else if (intent.getAction().equals(mBroadcastUnsnoozeSound)) {
                Log.i(TAG, "Phone received unsnoozed");
                CheckBox checkBox = getCheckboxFromAudioLabel(intent.getStringExtra(AUDIO_LABEL));
                SoundNotification soundNotification = SOUNDS_MAP.get(intent.getStringExtra(AUDIO_LABEL));
                if (soundNotification != null) {
                    soundNotification.isSnoozed = false;
                }
                if (checkBox == null) {
                    return;
                }
                if (checkBox.getText().toString().contains("Snoozed")) {
                    // If the sound is currently snoozed
                    int snoozeTextIndex = checkBox.getText().toString().indexOf("(Snoozed)");
                    String soundLabel = checkBox.getText().toString().substring(0, snoozeTextIndex);
                    checkBox.setText(soundLabel);
//                    checkBox.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
                return (CheckBox) findViewById(R.id.utensils_and_cutlery);
            case CHOPPING:
                return (CheckBox) findViewById(R.id.chopping);
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
            case DOG_BARK:
                return (CheckBox) findViewById(R.id.dog_bark);
            case DRILL:
                return (CheckBox) findViewById(R.id.drill);
            case VACUUM:
                return (CheckBox) findViewById(R.id.vacuum);
            case BABY_CRY:
                return (CheckBox) findViewById(R.id.baby_crying);
            case SHAVER:
                return (CheckBox) findViewById(R.id.shaver);
            case TOOTHBRUSH:
                return (CheckBox) findViewById(R.id.tooth_brush);
            case BLENDER:
                return (CheckBox) findViewById(R.id.blender);
            case DOORBELL:
                return (CheckBox) findViewById(R.id.door_bell);
            case TOILET_FLUSH:
                return (CheckBox) findViewById(R.id.toilet_flush);
            case SNORING:
                return (CheckBox) findViewById(R.id.snoring);
            case HAMMERING:
                return (CheckBox) findViewById(R.id.hammering);
            case SAW:
                return (CheckBox) findViewById(R.id.saw);
            case CAT_MEOW:
                return (CheckBox) findViewById(R.id.cat_meow);
            case HAIR_DRYER:
                return (CheckBox) findViewById(R.id.hair_dryer);
            case ALARM_CLOCK:
                return (CheckBox) findViewById(R.id.alarm_clock);
            case COUGHING:
                return (CheckBox) findViewById(R.id.coughing);
            case TYPING:
                return (CheckBox) findViewById(R.id.typing);
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
        super.onActivityResult(requestCode, resultCode, data);
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
    public void onLocationAwarenessClick(View view) {

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
                Log.d(TAG, "convertView is null");
                holder = new ViewHolder();
                LayoutInflater inflater =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                Log.d(TAG, "convertView is " + convertView.getTag());
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

//        private void loadFragment(Fragment fragment) {
//        // load fragment
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.frame_container, fragment);
//        transaction.addToBackStack(null);
//        transaction.commit();
//    }
    /**
     * loading fragment into FrameLayout
     *
     * @param fragment a new fragment
     */
    private void loadFragment(Fragment fragment) {
        //switching fragment
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    /*
    * Set different mode based on user's preferences.
    * Currently there are two modes:
    *   1. High Accuracy Mode/ Less sounds recognized: 11 sounds available
    *   2. Low Accuracy Mode/ More sounds recognized: 30 sounds available
    *
    * @param mode 1 for high accuracy, 2 for low accuracy
    * */
    private void setAccuracyMode(int mode) {
        CheckBox checkBoxToDisable;
        if (mode == HIGH_ACCURACY_MODE) {
            for (Integer sound: lowAccuracyList) {
                checkBoxToDisable = (CheckBox)findViewById(sound);
                checkBoxToDisable.setVisibility(View.GONE);
            }
            // some config for padding
            findViewById(R.id.category_5).setVisibility(View.GONE);
            findViewById(R.id.padding_top).setPadding(0, 150, 0, 0);
            findViewById(R.id.padding_bottom).setPadding(0, 0, 0, 80);
        }
    }
}
