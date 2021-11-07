/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.wearable.sound.utils;

import static com.wearable.sound.utils.Constants.ARCHITECTURE;
import static com.wearable.sound.utils.Constants.AUDIO_TRANSMISSION_STYLE;
import static com.wearable.sound.utils.Constants.PHONE_WATCH_ARCHITECTURE;
import static com.wearable.sound.utils.HelperUtils.longToBytes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.wearable.Wearable;

import java.util.Set;

/**
 * A helper class to provide methods to record audio input from the MIC and send raw audio to phone
 * constantly.
 */
public class SoundRecorder {

    private static final String TAG = "SoundRecorder";
    public static final String AUDIO_MESSAGE_PATH = "/audio_message";

    private static final int RECORDING_RATE = 16000; // (Hz == number of sample per second) can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDING_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);
    private static final int TRANSMISSION_SIZE_IN_BYTES = Math.max(5360, RECORDING_RATE * 330 / 10000) * 2;

    private final Context mContext;
    private State mState;
    private Set<String> connectedHostIds;

    /**
     * Instead of using AsyncTask (deprecated), use thread to handle recording task
     * Sample code: https://github.com/android/wear-os-samples/blob/master/DataLayer/Wearable/src/main/java/com/example/android/wearable/datalayer/MainActivity.java
     */
    private AudioRecord mAudioRecord;
    private Thread recordingThread;

    enum State {
        IDLE, RECORDING
    }

    /**
     * @param context : context
     */
    public SoundRecorder(Context context) {
        this.mContext = context;
        this.mState = State.IDLE;
        this.mAudioRecord = null;
        this.recordingThread = null;
        // TODO: for later release, make predictions within the watch itself for WATCH_ONLY_ARCHITECTURE
    }

    /**
     * Starts recording from the MIC.
     *
     * @param connectedHostIds:
     */
    public void startRecording(Set<String> connectedHostIds) {
        Log.i(TAG, "Current Architecture: " + ARCHITECTURE);
        Log.i(TAG, "Transportation Mode: " + AUDIO_TRANSMISSION_STYLE);
        if (mState != State.IDLE) {
            Log.i(TAG, "Requesting to start recording while state was not IDLE");
            return;
        }
        this.connectedHostIds = connectedHostIds;

        // initialize an audio recorder here?
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        this.mAudioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(RECORDING_RATE)
                        .setChannelMask(CHANNEL_IN)
                        .setEncoding(FORMAT)
                        .build()
                ).setBufferSizeInBytes(RECORDING_BUFFER_SIZE)
                .build();

        if (this.mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            // check for proper initialization
            Log.e(TAG, "Error initializing AudioRecord");
            return;
        }

        this.mAudioRecord.startRecording();
        Log.d(TAG, "Recording started with AudioRecord");

        this.mState = State.RECORDING;

        this.recordingThread = new Thread(this::processAudioData);
        recordingThread.start();
    }

    /**
     * Stop the MIC from recording.
     */
    public void stopRecording() {
        if (this.mAudioRecord != null) {
            this.mState = State.IDLE; // triggering recordingThread to exit while loop
        }
    }


    /**
     * Called inside Runnable of recordingThread. Processing the audio data recorded
     */
    private void processAudioData() {
        // buffer just enough bytes to for phone to make one prediction string
        byte[] data = new byte[TRANSMISSION_SIZE_IN_BYTES];
        while (this.mState == State.RECORDING) {
            long recordTime = System.currentTimeMillis();
            this.mAudioRecord.read(data, 0, data.length);

            // TODO: only having phone watch architecture for now
            if (ARCHITECTURE.equals(PHONE_WATCH_ARCHITECTURE)) {
                // send raw bytes directly to phone for now
                Log.d(TAG, "Processing data for phone watch architecture...");
                sendRawAudioToPhone(data, recordTime);
            }
        }

        // cleaning up
        this.mAudioRecord.stop();
        this.mAudioRecord.release();
        Log.d(TAG, "Stop recording. Cleaning up...");

        this.mAudioRecord = null;
        this.recordingThread = null;
    }

    /**
     * @param buffer     : array of audio bytes
     * @param recordTime : timestamp
     */
    private void sendRawAudioToPhone(byte[] buffer, long recordTime) {
        Log.d(TAG, "Sending this raw buffer array with size of " + buffer.length);
        // send the raw bytes to phone in this format:
        //  [ --- record timestamp ---, --------- audio data --------- ]
        byte[] timestampData = longToBytes(recordTime);
        byte[] data = new byte[timestampData.length + buffer.length];
        System.arraycopy(timestampData, 0, data, 0, timestampData.length);
        System.arraycopy(buffer, 0, data, timestampData.length, buffer.length);
        for (String connectedHostId : this.connectedHostIds) {
            Wearable.getMessageClient(this.mContext)
                    .sendMessage(connectedHostId, AUDIO_MESSAGE_PATH, data);
        }
    }
}
