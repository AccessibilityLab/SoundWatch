package com.wearable.sound.utils;

import static com.wearable.sound.datalayer.DataLayerListenerService.DBLEVEL_THRES;
import static com.wearable.sound.utils.HelperUtils.db;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.wearable.sound.models.SoundPrediction;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains methods to extract audio features or make predictions based on either audio bytes or audio features
 */
public class AudioProcessors {

    private static final String TAG = "Phone/AudioProcessors";

    // Predicting model and label file
    private final Interpreter tfLite;
    private static final String MODEL_FILENAME = "file:///android_asset/sw_model_v2.tflite";
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";
    private static final String INTERESTED_LABEL_FILENAME = "file:///android_asset/interested_labels.txt";
    private final List<String> labels;
    private final Set<String> interestedLabels;

    private Python py;
    private PyObject pythonModule;

    private static final int NUM_FRAMES = 32;  // Frames in input mel-spectrogram patch.
    private static final int NUM_BANDS = 64;   // Frequency bands in input mel-spectrogram patch.
    private static final int RECORDING_RATE = 16000; // must match with Wearable.SoundRecorder
    // note: vggish model require a buffer of minimum 5360 bytes to return a non-null result;
    //  that's equivalent to ~330ms of data with recording rate of 16kHz;
    //  for model v2, the buffer size is intended to be ~320ms
    private static final int BUFFER_SIZE = Math.max(5360, RECORDING_RATE * 330 / 10000);

    private static final float PREDICTION_THRES = 0.4F;

    private final Context context;

    public AudioProcessors(Context context) {
        this.context = context;

        // Load label file
        this.labels = new ArrayList<>();
        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.context.getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                this.labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file of " + actualLabelFilename, e);
        }

        // Load tflite model
        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            this.tfLite = new Interpreter(loadModelFile(this.context.getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // load interested label filename
        this.interestedLabels = new HashSet<>();
        String actualInterestedLabelFilename = INTERESTED_LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.context.getAssets().open(actualInterestedLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                this.interestedLabels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file of " + actualInterestedLabelFilename, e);
        }
    }

    /**
     * Take in an array audio and loudness of the sounds, return a list of SoundPrediction
     *
     * @param input1D        : an array of floats representing audio features
     * @param db             : the loudness of the sound array
     * @param multipleSounds : true if want to return a list of sound predictions; false otherwise
     * @return a list of sound predictions whose confidence >= threshold, sorted in decreasing order
     */
    public List<SoundPrediction> predictSoundsFromAudioFeatures(float[] input1D, double db, boolean multipleSounds) {
        if (Constants.DEBUG_LOG) Log.i(TAG, "Predicting sounds from audio features");

        if (db < DBLEVEL_THRES) {
            if (Constants.DEBUG_LOG) Log.i(TAG, "Loudness < DBLEVEL_THRES");
            return null;
        }

        // Reshape to dimensions of model input
        int count = 0;
        float[][][] input3D = new float[1][NUM_FRAMES][NUM_BANDS];
        for (int j = 0; j < NUM_FRAMES; j++) {
            for (int k = 0; k < NUM_BANDS; k++) {
                input3D[0][j][k] = input1D[count];
                count++;
            }
        }

        if (Constants.DEBUG_LOG) Log.d(TAG, "Input3D is " + Arrays.deepToString(input3D));

        // Run inference
        float[][] output = new float[1][this.labels.size()];
        tfLite.run(input3D, output);
        if (Constants.DEBUG_LOG) Log.d(TAG, "Output running tflite is " + Arrays.deepToString(output));

        // TODO: re-implement test end-to-end latency

        List<SoundPrediction> predictions = new ArrayList<>();
        if (multipleSounds) {
            for (int i = 0; i < this.labels.size(); i++) {
                if (output[0][i] >= PREDICTION_THRES && this.interestedLabels.contains(labels.get(i))) {
                    predictions.add(new SoundPrediction(labels.get(i), output[0][i]));
                }
            }
            // Sort the predictions by value in decreasing order
            predictions.sort(Collections.reverseOrder());
        } else {
            // Only use argmax
            // Find max and argmax
            float max = output[0][0];
            int argmax = 0;
            for (int i = 0; i < this.labels.size(); i++) {
                if (max < output[0][i]) {
                    max = output[0][i];
                    argmax = i;
                }
            }

            if (max >= PREDICTION_THRES) {
                predictions.add(new SoundPrediction(labels.get(argmax), output[0][argmax]));
            }
        }

        if (Constants.DEBUG_LOG) Log.i(TAG, LocalTime.now() + "        " + ": Predictions are " + predictions.toString());
        return predictions;
    }

    /**
     * Take in an array of raw audio shorts and return a list of SoundPrediction
     *
     * @param rawData        : an array of shorts representing the raw audio bytes
     * @param multipleSounds : true if want to return a list of sound predictions; false otherwise
     * @return a list of sound predictions whose confidence >= threshold
     * - empty if there are no sounds with confidence >= threshold
     * - null if something went wrong
     */
    public List<SoundPrediction> predictSoundFromRawAudio(short[] rawData, boolean multipleSounds) {
        if (Constants.DEBUG_LOG) Log.i(TAG, "Predicting sounds from raw audio shorts");

        if (rawData.length != BUFFER_SIZE) {
            if (Constants.DEBUG_LOG) Log.d(TAG, "Invalid buffer size, not enough to make predictions");
            return null;
        }

        double db = db(rawData);
        if (Constants.DEBUG_LOG) Log.d(TAG, "DB of data: " + db + "| DB_thresh: " + DBLEVEL_THRES);
        if (db >= DBLEVEL_THRES) {
            // extract audio features from raw bytes
            float[] input1D = extractAudioFeaturesRawAudio(rawData);
            if (input1D == null) {
                if (Constants.DEBUG_LOG) Log.d(TAG, "Cannot extract features from raw audio");
                return null;
            }
            if (Constants.DEBUG_LOG) Log.d(TAG, "Input1D is " + Arrays.toString(input1D));

            return predictSoundsFromAudioFeatures(input1D, db, multipleSounds);
        }

        if (Constants.DEBUG_LOG) Log.i(TAG, "Loudness < DBLEVEL_THRES");
        return null;
    }

    /**
     * Extracting audio features from the raw audio shorts
     *
     * @param rawData : an array of shorts representing the raw audio bytes
     * @return an array of floats representing audio features
     */
    public float[] extractAudioFeaturesRawAudio(short[] rawData) {
        if (rawData.length != BUFFER_SIZE) {
            // Sanity check, because sound has to be exactly bufferElements2Rec elements
            if (Constants.DEBUG_LOG) Log.d(TAG, "Invalid buffer size: expect " + BUFFER_SIZE + "; given " + rawData.length);
            return null;
        }

        try {
            if (db(rawData) >= DBLEVEL_THRES) {
                if (Constants.DEBUG_LOG) Log.d(TAG, "Within threshold.");

                long startTimePython = System.currentTimeMillis();
                if (py == null || pythonModule == null) {
                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(this.context));
                    }

                    py = Python.getInstance();
                    pythonModule = py.getModule("main");
                    if (Constants.DEBUG_LOG) Log.i(TAG, "Time elapsed after init Python modules: "
                            + (System.currentTimeMillis() - startTimePython));
                }

                // Get MFCC features
                if (Constants.DEBUG_LOG) Log.d(TAG, "Sending to python an array length of " + rawData.length);
                PyObject mfccFeatures = pythonModule.callAttr("audio_samples", Arrays.toString(rawData));

                if (Constants.DEBUG_LOG) Log.i(TAG, "Time elapsed after running Python " + (System.currentTimeMillis() - startTimePython));

                // Parse features into a float array
                String inputString = mfccFeatures.toString();
                inputString = inputString.replace("jarray('F')([", "").replace("])", "");
                String[] inputStringArr = inputString.split(", ");
                if (inputStringArr.length == 0) {
                    return null;
                }

                float[] result = new float[NUM_BANDS * NUM_FRAMES];
                for (int i = 0; i < result.length; i++) {
                    if (inputStringArr[i].isEmpty()) {
                        return null;
                    }
                    result[i] = Float.parseFloat(inputStringArr[i]);
                }
                return result;
            }

            return null;
        } catch (PyException e) {
            if (Constants.DEBUG_LOG) Log.i(TAG, "Something went wrong parsing to MFCC feature");
            return null;
        }
    }

    /**
     * Memory-map the model file in Assets.
     */
    private ByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
