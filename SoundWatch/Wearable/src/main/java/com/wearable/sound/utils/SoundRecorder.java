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

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

//import com.chaquo.python.PyException;
//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;
import com.wearable.sound.ui.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.wearable.sound.ui.activity.MainActivity.mBroadcastSoundPrediction;
import static com.wearable.sound.utils.Constants.AUDIO_LABEL;
import static com.wearable.sound.utils.HelperUtils.convertByteArrayToShortArray;
import static com.wearable.sound.utils.HelperUtils.db;
import static com.wearable.sound.utils.HelperUtils.longToBytes;
import static com.wearable.sound.utils.Constants.*;

/**
 * A helper class to provide methods to record audio input from the MIC and send raw audio to phone
 * constantly.
 */
public class SoundRecorder {

    private static final String TAG = "SoundRecorder";
    private static final String DEBUG_TAG = "FromSoftware";
    private static final int RECORDING_RATE = 16000; // (Hz == number of sample per second) can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // this might vary because it will optimize for difference device (should not make it fixed?)
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);
    public static final String AUDIO_MESSAGE_PATH = "/audio_message";
    private final Context mContext;
    private State mState = State.IDLE;

    private AsyncTask<Void, Void, Void> mRecordingAsyncTask;

    private Set<String> connectedHostIds;

    enum State {
        IDLE, RECORDING
    }

    /**
     * @param context : context
     */
    public SoundRecorder(Context context) {
//        mOutputFileName = outputFileName;
        mContext = context;
        if (ARCHITECTURE.equals(WATCH_ONLY_ARCHITECTURE)) {
            // TODO: for later, make predictions within the watch itself
        }
    }

    /**
     * Starts recording from the MIC.
     *
     * @param connectedHostIds:
     */
    public void startRecording(Set<String> connectedHostIds) {
        Log.i(TAG, "Current Architecture: " + ARCHITECTURE);
        Log.i(TAG, "Transportation Mode: " + AUDIO_TRANMISSION_STYLE);
        if (mState != State.IDLE) {
            return;
        }
        this.connectedHostIds = connectedHostIds;
        mRecordingAsyncTask = new RecordAudioAsyncTask(this);

        mRecordingAsyncTask.execute();
    }

    public void stopRecording() {
        if (mRecordingAsyncTask != null) {
            mRecordingAsyncTask.cancel(true);
        }
    }

    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    private static class RecordAudioAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<SoundRecorder> mSoundRecorderWeakReference;

        RecordAudioAsyncTask(SoundRecorder context) {
            mSoundRecorderWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

            if (soundRecorder != null) {
                soundRecorder.mState = State.RECORDING;
            }
        }


        @Override
        protected Void doInBackground(Void... params) {
            mSoundRecorderWeakReference.get();
            AudioRecord mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
            try {
                final byte[] buffer = new byte[BUFFER_SIZE];
                mAudioRecord.startRecording();
                while (!isCancelled()) {
                    mAudioRecord.read(buffer, 0, buffer.length);
                    if (AUDIO_TRANMISSION_STYLE.equals(RAW_AUDIO_TRANSMISSION) &&
                            (ARCHITECTURE.equals(PHONE_WATCH_ARCHITECTURE) ||
                                    ARCHITECTURE.equals(PHONE_WATCH_SERVER_ARCHITECTURE))) {
                        // For raw audio transmission, we need to send the buffer all the time
                        // Not waiting for the short buffer to build up
                        processAudioRecognition(null, buffer);
                    }
                }
            } catch (NullPointerException | IndexOutOfBoundsException e) {
                Log.e(TAG, "Failed to record data: " + e);
            } finally {
                mAudioRecord.release();
            }
            return null;
        }

        private void processAudioRecognition(List<Short> soundBuffer, byte[] buffer) {
            long recordTime = System.currentTimeMillis();
            switch (ARCHITECTURE) {
                case WATCH_ONLY_ARCHITECTURE:
                    break;
                case WATCH_SERVER_ARCHITECTURE:
                    switch (AUDIO_TRANMISSION_STYLE) {
                        case AUDIO_FEATURES_TRANSMISSION:
                            sendSoundFeaturesToServer(soundBuffer, recordTime);
                            break;
                        case RAW_AUDIO_TRANSMISSION:
                            sendRawAudioToServer(soundBuffer, recordTime);
                            break;
                        default:
                            Log.i(TAG, "Invalid architecture");
                            break;
                    }
                    break;
                case PHONE_WATCH_ARCHITECTURE:
                case PHONE_WATCH_SERVER_ARCHITECTURE:
                    /**
                     *
                     This is the same because phone is in charge of which mode it wants to run
                     */
                    switch (AUDIO_TRANMISSION_STYLE) {
                        case AUDIO_FEATURES_TRANSMISSION:
//                            sendSoundFeaturesToPhone(soundBuffer, recordTime);
                            break;
                        case RAW_AUDIO_TRANSMISSION:
                            sendRawAudioToPhone(buffer, recordTime);
                            break;
                        default:
                            Log.i(TAG, "Invalid transmission style");
                            break;
                    }
                    break;
                default:
                    Log.i(TAG, "Invalid Architecture");
                    break;
            }
        }

        /**
         * @param buffer : array of audio bytes
         * @param recordTime : timestamp
         */
        private void sendRawAudioToPhone(byte[] buffer, long recordTime) {
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
            if (TEST_E2E_LATENCY) {
                byte[] currentTimeData = longToBytes(recordTime);
                byte[] data = new byte[currentTimeData.length + buffer.length];
                System.arraycopy(currentTimeData, 0, data, 0, currentTimeData.length);
                System.arraycopy(buffer, 0, data, currentTimeData.length, buffer.length);
                for (String connectedHostId : soundRecorder.connectedHostIds) {
                    Wearable.getMessageClient(soundRecorder.mContext)
                            .sendMessage(connectedHostId, AUDIO_MESSAGE_PATH, data);
                }
            } else {
                for (String connectedHostId : soundRecorder.connectedHostIds) {
                    Wearable.getMessageClient(soundRecorder.mContext)
                            .sendMessage(connectedHostId, AUDIO_MESSAGE_PATH, buffer);
                }
            }
        }

        /**
         * @param soundBuffer: list of shorts representing the audio
         * @param recordTime : timestamp
         */
        private void sendSoundFeaturesToServer(List<Short> soundBuffer, long recordTime) {
            try {
                Log.i(TAG, "sendSoundFeaturesToServer()");
                JSONObject jsonObject = new JSONObject();
                SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

//                float[] features = soundRecorder.extractAudioFeatures(soundBuffer);
                // TODO: implement this later. Not sending features to server for now (to reduce app size)
                float[] features = null;
                if (features == null) {
                    Log.i(TAG, "Received Null Features");
                    return;
                }
                jsonObject.put("data", new JSONArray(features));
                jsonObject.put("db", Math.abs(db(soundBuffer)));
                jsonObject.put("time", "" + System.currentTimeMillis());

                if (TEST_E2E_LATENCY)
                    jsonObject.put("record_time", Long.toString(recordTime));

                Log.i(TAG, "Data sent to server: " + features.length);
                MainActivity.mSocket.emit("audio_feature_data", jsonObject);

            } catch (JSONException e) {
                Log.i(TAG, "Failed sending sound features to server");
                e.printStackTrace();
            }
        }

        /**
         * @param soundBuffer: list of shorts representing the audio
         * @param recordTime : timestamp
         */
        private void sendRawAudioToServer(List<Short> soundBuffer, long recordTime) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", new JSONArray(soundBuffer));
                jsonObject.put("time", "" + System.currentTimeMillis());
                if (TEST_E2E_LATENCY) {
                    jsonObject.put("record_time", recordTime);
                }
                Log.i(TAG, "Sending audio data: " + soundBuffer.size());
                MainActivity.mSocket.emit("audio_data", jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

            if (soundRecorder != null) {
                soundRecorder.mState = State.IDLE;
                soundRecorder.mRecordingAsyncTask = null;
            }
        }

        @Override
        protected void onCancelled() {
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

            if (soundRecorder != null) {
                if (soundRecorder.mState == State.RECORDING) {
                    Log.d(TAG, "Stopping the recording ...");
                    soundRecorder.mState = State.IDLE;
                } else {
                    Log.w(TAG, "Requesting to stop recording while state was not RECORDING");
                }
                soundRecorder.mRecordingAsyncTask = null;
            }
        }
    }
}
