package com.watch.python.sound;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends WearableActivity {

    public static final String TAG = "WatchSoundDebug";

    private static final int RECORDER_SAMPLERATE = 16000;
    private static final float PREDICTION_THRES = 0.5F;
    private static final double DBLEVEL_THRES = -33.0;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private Interpreter tfLite;
    private static final String MODEL_FILENAME = "file:///android_asset/example_model.tflite";
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";

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

    int BufferElements2Rec = 16000;
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    protected Python py;
    PyObject pythonModule;

    private float [] input1D = new float [6144];
    private float [][][][] input4D = new float [1][96][64][1];
    private float[][] output = new float[1][30];

    Vibrator vib;

    String[] permissions = new String[]{
            //Manifest.permission.INTERNET,
            //Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.VIBRATE,
            Manifest.permission.RECORD_AUDIO,
    };

@Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        //Start an always running background service
        startService(new Intent(this, BackgroundService.class));

        // Set the UI
        setContentView(R.layout.activity_main);
        (findViewById(R.id.wearable_list_layout)).setVisibility(View.GONE);

        //Ask permissions for audio recording
        checkPermissions();

        //Initialize audio recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        //Initialize python module
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

        //Start recording
        recorder.startRecording();
        isRecording = true;

        //Start predicting sounds...
        recordingThread = new Thread(new Runnable() {
            public void run() {
                predictSounds();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();

        //Initialize vibrator
        vib = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
}


    @Override
    protected void onPause(){   //First indication that user is leaving your activity -- doesn't mean the activity is destroyed
        Log.d(TAG, "onPause()");
        super.onPause();
    }
    @Override
    protected void onResume(){   //App comes to the foreground until user focus is taking away, when onPause is called

        Log.d(TAG, "onResume()");
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
        super.onStop();
    }
    @Override
    protected void onDestroy(){

        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void predictSounds() {
        short sData[] = new short[BufferElements2Rec];

        while (isRecording) {
            // Read input from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);

            if(db(sData) >= DBLEVEL_THRES) {

                //Get MFCC features
                PyObject mfccFeatures = pythonModule.callAttr("audio_samples", Arrays.toString(sData));   //System.out.println("Sending to python: " + Arrays.toString(sData));

                //Parse features into a float array
                String inputString = mfccFeatures.toString();
                inputString = inputString.replace("jarray('F')([", "").replace("])", "");
                String[] inputStringArr = inputString.split(", ");
                for (int i = 0; i < 6144; i++) input1D[i] = Float.parseFloat(inputStringArr[i]);

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

                if(max > PREDICTION_THRES) {
                    //Get label and confidence
                    final String prediction = labels.get(argmax);
                    final String confidence = String.format("%,.2f", max);

                    //Show label
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView mainDisplay = findViewById(R.id.soundDisplay);
                            mainDisplay.setText(prediction + " (" + confidence + ")");
                            vib.vibrate(200);
                        }
                    });
                }
            }
        }
    }

    private double db(short[] data) {
        double rms = 0.0;
        for (int i = 0; i < data.length; i++) {
            rms += Math.abs(data[i]);
        }
        rms = rms/data.length;
        return 20 * Math.log10(rms/32768.0);
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
    }

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
}
