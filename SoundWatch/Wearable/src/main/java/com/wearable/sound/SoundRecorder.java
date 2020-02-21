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

package com.wearable.sound;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;

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
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

/**
 * A helper class to provide methods to record audio input from the MIC to the internal storage
 * and to playback the same recorded audio file.
 */
public class SoundRecorder {

    private static final String TAG = "SoundRecorder";
    private static final int RECORDING_RATE = 16000; // can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord
            .getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);
    public static final String AUDIO_MESSAGE_PATH = "/audio_message";
    private static final double DBLEVEL_THRES = -35.0;
    int BufferElements2Rec = 16000;
    private static final String SNOOZE_LABEL = "Snooze";
    private static final String SNOOZE_TIME_LABEL = "Snooze Time";
    private static final String[] SNOOZE_CHOICES = {"5 mins", "10 mins", "1 hour", "1 day", "Forever"};


    private List<Short> soundBuffer = new ArrayList<>();
    private float [] input1D = new float [6144];
    private float [][][][] input4D = new float [1][96][64][1];
    private float[][] output = new float[1][30];
    private final String mOutputFileName;
    private List<String> labels = new ArrayList<String>();
    private Interpreter tfLite;
    private static final String MODEL_FILENAME = "file:///android_asset/example_model.tflite";
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final float PREDICTION_THRES = 0.5F;
    private static final String CHANNEL_ID = "SOUNDWATCH";
//    private final AudioManager mAudioManager;
//    private final Handler mHandler;
    private final Context mContext;
    private State mState = State.IDLE;

    private AsyncTask<Void, Void, Void> mRecordingAsyncTask;
    private AsyncTask<Void, Void, Void> mPlayingAsyncTask;

    private Set<String> connectedHostIds;

    protected Python py;
    PyObject pythonModule;

    enum State {
        IDLE, RECORDING, PLAYING
    }

    public SoundRecorder(Context context, String outputFileName) {
        mOutputFileName = outputFileName;
        mContext = context;
        if (MainActivity.ARCHITECTURE.equals(MainActivity.WATCH_ONLY_ARCHITECTURE)) {
            //Load labels
            String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(mContext.getAssets().open(actualLabelFilename)));
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
                tfLite = new Interpreter(loadModelFile(mContext.getAssets(), actualModelFilename));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

    /**
     * Starts recording from the MIC.
     */
    public void startRecording() {
        if (mState != State.IDLE) {
            return;
        }

        mRecordingAsyncTask = new RecordAudioAsyncTask(this);

        mRecordingAsyncTask.execute();
    }

    public void startRecording(Set<String> connectedHostIds) {
        Log.i(TAG, "Current Architecture: " + MainActivity.ARCHITECTURE);
        Log.i(TAG, "Tranportation Mode: " + MainActivity.AUDIO_TRANMISSION_STYLE);
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

    /**
     * Cleans up some resources related to {@link AudioTrack} and {@link AudioRecord}
     */
    public void cleanup() {
        stopRecording();
    }
    /**
     * Code to extract audio features
     */
    private double db(short[] data) {
        double rms = 0.0;
        for (int i = 0; i < data.length; i++) {
            rms += Math.abs(data[i]);
        }
        rms = rms/data.length;
        return 20 * Math.log10(rms/32768.0);
    }

    public static double db(List<Short> soundBuffer) {
        double rms = 0.0;
        for (int i = 0; i < soundBuffer.size(); i++) {
            rms += Math.abs(soundBuffer.get(i));
        }
        rms = rms/soundBuffer.size();
        return 20 * Math.log10(rms/32768.0);
    }

    private String predictSoundsFromRawAudio(List<Short> soundBuffer) {
        if (soundBuffer.size() != 16000) {
            soundBuffer = new ArrayList<>();
            return "Invalid audio size";
        }
        short[] sData = new short[16000];
        for (int i = 0; i < soundBuffer.size(); i++) {
            sData[i] = soundBuffer.get(i);
        }
        try {
            if (db(sData) >= DBLEVEL_THRES && sData.length > 0) {
                if (py == null || pythonModule == null) {
                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(this.mContext));
                    }

                    py = Python.getInstance();
                    pythonModule = py.getModule("main");
                }

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
                    createAudioLabelNotification(new AudioLabel(prediction, confidence, java.time.LocalTime.now().toString(), Double.toString(db(sData))));
                    return prediction + ": " + (Double.parseDouble(confidence) * 100) + "%                           " + LocalTime.now();
                }
            }
        } catch (PyException e) {
            Log.i(TAG, "Something went wrong parsing to MFCC feature");
            return "Something went wrong parsing to MFCC feature";
        }
        Log.i(TAG, "Sending Mocked Sound");
        createAudioLabelNotification(new AudioLabel("Speech", "0.9", java.time.LocalTime.now().toString(), Double.toString(db(sData))));
//        Log.i(TAG, "Unrecognized Sound");
        return "Unrecognized sound" + "                           " + LocalTime.now();
    }

    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    private float[] extractAudioFeatures(List<Short> soundBuffer) {
        float [] input1D = new float [6144];
        if (soundBuffer.size() != 16000) {
            // Sanity check, because sound has to be exactly 16000 elements
            soundBuffer = new ArrayList<>();
            return null;
        }
        short[] sData = new short[16000];
        for (int i = 0; i < soundBuffer.size(); i++) {
            sData[i] = soundBuffer.get(i);
        }
        try {
            if (db(sData) >= DBLEVEL_THRES && sData.length > 0) {
                // Lazily load python module to faster boot up processing
                if (py == null || pythonModule == null) {
                    synchronized (this) {
                        if (!Python.isStarted()) {
                            Python.start(new AndroidPlatform(this.mContext));
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
                for (int i = 0; i < 6144; i++) {
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
    private static class RecordAudioAsyncTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<SoundRecorder> mSoundRecorderWeakReference;
        private AudioRecord mAudioRecord;

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
            final SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
            BufferedOutputStream bufferedOutputStream = null;
            try {
                bufferedOutputStream = new BufferedOutputStream(
                        soundRecorder.mContext.openFileOutput(
                                soundRecorder.mOutputFileName,
                                Context.MODE_PRIVATE));
                byte[] buffer = new byte[BUFFER_SIZE];
                mAudioRecord.startRecording();
                while (!isCancelled()) {
                    int read = mAudioRecord.read(buffer, 0, buffer.length);
                    short[] shorts = convertByteArrayToShortArray(buffer);
                    if (soundRecorder.soundBuffer.size() == 16000) {
                        final List<Short> tempBuffer = soundRecorder.soundBuffer;

                        processAudioRecognition(tempBuffer, buffer);
                        soundRecorder.soundBuffer = new ArrayList<>();
                        // TODO: Try spawning a thread here to see if it is faster
////                        new Thread() {
////                            @Override
////                            public void run() {
////                                sendSoundFeaturesToServer(tempBuffer);
////                            }
////                        }.start();

                    }
                    if (soundRecorder.soundBuffer.size() < 16000) {
                        for (short num : shorts) {
                            if (soundRecorder.soundBuffer.size() == 16000) {
                                final List<Short> tempBuffer = soundRecorder.soundBuffer;
                                processAudioRecognition(tempBuffer, buffer);
                                soundRecorder.soundBuffer = new ArrayList<>();
                            }
                            soundRecorder.soundBuffer.add(num);
                        }
                    }
                    bufferedOutputStream.write(buffer, 0, read);
                }
            } catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
                Log.e(TAG, "Failed to record data: " + e);
            } finally {
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                mAudioRecord.release();
                mAudioRecord = null;
            }
            return null;
        }

        private void processAudioRecognition(List<Short> soundBuffer, byte[] buffer) {
            switch (MainActivity.ARCHITECTURE) {
                case MainActivity.WATCH_ONLY_ARCHITECTURE:
                    predictSoundsFromRawAudio(soundBuffer);
                    break;
                case MainActivity.WATCH_SERVER_ARCHITECTURE:
                    switch (MainActivity.AUDIO_TRANMISSION_STYLE) {
                        case MainActivity.AUDIO_FEATURES_TRANSMISSION:
                            sendSoundFeaturesToServer(soundBuffer);
                            break;
                        case MainActivity.RAW_AUDIO_TRANSMISSION:
                            sendRawAudioToServer(soundBuffer);
                            break;
                        default:
                            sendSoundFeaturesToServer(soundBuffer);
                            break;
                    }
                    break;
                case MainActivity.PHONE_WATCH_ARCHITECTURE:
                    switch (MainActivity.AUDIO_TRANMISSION_STYLE) {
                        case MainActivity.AUDIO_FEATURES_TRANSMISSION:
                            sendSoundFeaturesToPhone(soundBuffer);
                            break;
                        case MainActivity.RAW_AUDIO_TRANSMISSION:
                            sendRawAudioToPhone(buffer);
                            break;
                        default:
                            sendSoundFeaturesToServer(soundBuffer);
                            break;
                    }
                    break;
                case MainActivity.PHONE_WATCH_SERVER_ARCHITECTURE:
                    /**
                     *
                     This is the same because phone is in charge of which mode it wants to run
                     */
                    switch (MainActivity.AUDIO_TRANMISSION_STYLE) {
                        case MainActivity.AUDIO_FEATURES_TRANSMISSION:
                            sendSoundFeaturesToPhone(soundBuffer);
                            break;
                        case MainActivity.RAW_AUDIO_TRANSMISSION:
                            sendRawAudioToPhone(buffer);
                            break;
                        default:
                            sendSoundFeaturesToServer(soundBuffer);
                            break;
                    }
                    break;
                default:
                    Log.i(TAG, "Invalid Architecture");
                    break;
            }
        }

        private String predictSoundsFromRawAudio(List<Short> soundBuffer) {
            if (soundBuffer.size() != 16000) {
                return "Invalid audio size";
            }
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
            return soundRecorder.predictSoundsFromRawAudio(soundBuffer);
        }

        private void sendSoundFeaturesToPhone(List<Short> soundBuffer) {
            try {
                JSONObject jsonObject = new JSONObject();
                SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
                float[] features = soundRecorder.extractAudioFeatures(soundBuffer);

                if (features == null) {
                    return;
                }
                ByteArrayOutputStream bas = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(bas);
                for (float f : features)
                    ds.writeFloat(f);
                /**
                 * Send data in form of (db loudness + features array)
                 * where loudness is a double of 8 byte and features is a byte array representing
                 * array of shorts
                 */
                byte[] featuresData = bas.toByteArray();
                double db = Math.abs(db(soundBuffer));
                byte[] dbData = toByteArray(db);
                Log.i(TAG, "Loudness db sent from watch: " + db);

                byte[] data = new byte[dbData.length + featuresData.length];
                System.arraycopy(dbData, 0, data, 0, dbData.length);
                System.arraycopy(featuresData, 0, data, dbData.length, featuresData.length);

                for (String connectedHostId : soundRecorder.connectedHostIds) {
                    Log.d(TAG, "Sending audio data to phone");
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(soundRecorder.mContext)
                                    .sendMessage(connectedHostId, AUDIO_MESSAGE_PATH, data);
                }
            } catch (IOException e){
                Log.i(TAG, "ERROR occured while parsing float features to bytes");
            }
        }

        private void sendRawAudioToPhone(byte[] buffer) {
            SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
            for (String connectedHostId: soundRecorder.connectedHostIds) {
                    Log.d(TAG, "Sending audio data to phone");
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(soundRecorder.mContext)
                                    .sendMessage(connectedHostId, AUDIO_MESSAGE_PATH, buffer);
            }
        }

        private void sendSoundFeaturesToServer(List<Short> soundBuffer) {
            try {
                JSONObject jsonObject = new JSONObject();
                SoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

                float[] features = soundRecorder.extractAudioFeatures(soundBuffer);
                if (features == null) {
                    return;
                }
                jsonObject.put("data", new JSONArray(features));
                jsonObject.put("db", Math.abs(db(soundBuffer)));
                Log.i(TAG, "Data sent to server: "  + features.length);
                MainActivity.mSocket.emit("audio_feature_data", jsonObject);
            } catch (JSONException e) {
                Log.i(TAG, "Failed sending sound features to server");
                e.printStackTrace();
            }
        }

        private void sendRawAudioToServer(List<Short> soundBuffer) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", new JSONArray(soundBuffer));
                MainActivity.mSocket.emit("audio_data", jsonObject);
                Log.i(TAG, "Successfully send audio data from background");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private short[] convertByteArrayToShortArray(byte[] bytes) {
            short[] result = new short[bytes.length / 2];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
            return result;
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

    public class AudioLabel {
        String label;
        double confidence;
        String time;
        String db;
        public AudioLabel(String label, String confidence, String time, String db) {
            this.label = label;
            this.confidence = Double.parseDouble(confidence);
            this.time = time;
            this.db = db;
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
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//            channel.setVibrationPattern(new long[]{2000});
            notificationManager.createNotificationChannel(channel);
        }
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

    public void createAudioLabelNotification(AudioLabel audioLabel) {

        final int NOTIFICATION_ID = ((MyApplication) mContext.getApplicationContext()).getIntegerValueOfSound(audioLabel.label);

        //If it's blocked or app in foreground (to not irritate user), return
        if ((((MyApplication) mContext.getApplicationContext()).getBlockedSounds()).contains(NOTIFICATION_ID)
                || !(((MyApplication) mContext.getApplicationContext()).enabledSounds.contains(audioLabel.label))
                || (((MyApplication) mContext.getApplicationContext()).isAppInForeground()))
            return;

        Log.d(TAG, "generateBigTextStyleNotification()");
        if (!MainActivity.notificationChannelIsCreated) {
            createNotificationChannel();
            MainActivity.notificationChannelIsCreated = true;
        }

        Intent intent = new Intent(mContext, MainActivity.class);       //Just go the MainActivity for now. Replace with other activity if you want more actions.
        String[] dataPassed = {audioLabel.label, Double.toString(audioLabel.confidence), audioLabel.time};         //Adding data to be passed back to the main activity
        intent.putExtra("audio_label", dataPassed);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the RemoteInput.
        RemoteInput remoteInput =
                new RemoteInput.Builder(SnoozeSoundService.SNOOZE_TIME)
                        .setLabel(SNOOZE_TIME_LABEL)
                        // List of quick response choices for any wearables paired with the phone.
                        .setChoices(SNOOZE_CHOICES)
                        .build();

        Intent snoozeIntent = new Intent(mContext, SnoozeSoundService.class);
        snoozeIntent.putExtra(SnoozeSoundService.SOUND_ID, NOTIFICATION_ID);
        snoozeIntent.putExtra(SnoozeSoundService.SOUND_LABEL, audioLabel.label);
        snoozeIntent.putExtra(SnoozeSoundService.CONNECTED_HOST_IDS, convertSetToCommaSeparatedList(connectedHostIds));
        snoozeIntent.putExtra(SnoozeSoundService.SNOOZE_TIME, 10 * 60 * 1000);


        Log.i(TAG, "Main pass sound id to block " + NOTIFICATION_ID);
//        snoozeIntent.putExtra(SnoozeSoundService.SOUND_ID, Integer.toString(NOTIFICATION_ID));
        PendingIntent snoozeSoundPendingIntent = PendingIntent.getService(mContext, 0, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
        // lower portion of the Notification for easy action (only possible for one action).
        final NotificationCompat.Action.WearableExtender inlineActionForWear2 =
                new NotificationCompat.Action.WearableExtender()
                        .setHintDisplayActionInline(true)
                        .setHintLaunchesActivity(false);

        String db = audioLabel.db;
        if (db.contains("\\.")) {
            String[] parts = db.split("\\.");
            db = parts[0];
        } else {
            db = db.substring(0, 2);
        }

        NotificationCompat.Action snoozeAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_full_cancel,
                        SNOOZE_LABEL,
                        snoozeSoundPendingIntent)
                        .addRemoteInput(remoteInput)
                        // Informs system we aren't bringing up our own custom UI for a reply
                        // action.
                        .setShowsUserInterface(false)
                        // Allows system to generate replies by context of conversation.
                        .setAllowGeneratedReplies(true)
                        // Add WearableExtender to enable inline actions.
//                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
//                        .extend(inlineActionForWear2)
                        .build();

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(), CHANNEL_ID)
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

        /** Set Choices for Snooze **/
//                .addAction(snoozeAction)
////                .setCategory(Notification.CATEGORY_SOCIAL)
//
//                .extend(new NotificationCompat.WearableExtender()
//                        .setHintContentIntentLaunchesActivity(true));
        ;
        //.setOnlyAlertOnce(true);     //Only notify once (see notification ID below)
        //.addAction(snoozeAction)
        //.addAction(dismissAction);

        //JUGAAD: NOTIFICATION ID depends on the sound and the location so a particular sound in a particular location is only notified once until dismissed
        Log.d(TAG, "Notification Id: " + NOTIFICATION_ID);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.notify(NOTIFICATION_ID, notificationCompatBuilder.build());
        // TODO: not sure if we need this vibration
//        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
//        v.vibrate(350);
//        int notificationId = 001;

    }
}
