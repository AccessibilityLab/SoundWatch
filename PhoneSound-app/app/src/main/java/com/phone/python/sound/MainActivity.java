package com.phone.python.sound;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.phone.python.sound.R;

import android.media.MediaRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    int BufferElements2Rec = 16000;
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    protected Python py;
    PyObject pythonModule;

    String[] permissions = new String[]{
            //Manifest.permission.INTERNET,
            //Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.VIBRATE,
            Manifest.permission.RECORD_AUDIO,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        //Initialize python module
        py = Python.getInstance();
        pythonModule = py.getModule("main");

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                predictSounds();
            }
        }, "AudioRecorder Thread");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                recordingThread.start();
            }
        }, 1000);
    }

    private void predictSounds() {
        short sData[] = new short[BufferElements2Rec];

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);
            String sPy = new String(Arrays.toString(sData));                //System.out.println("Sending to python: " + sPy);
            final PyObject returnVal = pythonModule.callAttr("audio_samples", sPy);

            if(returnVal != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView mainDisplay = findViewById(R.id.mainDisplay);
                        mainDisplay.setMovementMethod(new ScrollingMovementMethod());
                        mainDisplay.append("\n" + returnVal.toString());
                    }
                });
            }
        }
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