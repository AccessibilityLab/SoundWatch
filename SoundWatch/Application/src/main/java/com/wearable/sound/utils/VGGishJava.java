package com.wearable.sound.utils;

/**
 * Convert the python code of VGGish to Java
 * Original source code: https://github.com/tensorflow/models/tree/master/research/audioset/vggish
 */
public class VGGishJava {

    /**
     * A bunch of constants/hyper-parameters
     * Equivalent to 'vggish_params.py'
     */
    private static final int NUM_FRAMES = 32;  // Frames in input mel-spectrogram patch.
    private static final int NUM_BANDS = 64;   // Frequency bands in input mel-spectrogram patch.
    private static final int EMBEDDING_SIZE = 128; // Size of embedding layer.

    private static final int SAMPLE_RATE = 16000;
    private static final double STFT_WINDOW_LENGTH_SECONDS = 0.025;
    private static final double STFT_HOP_LENGTH_SECONDS = 0.010;
    private static final int NUM_MEL_BINS = NUM_BANDS;
    private static final int MEL_MIN_HZ = 125;
    private static final int MEL_MAX_HZ = 7500;
    private static final double LOG_OFFSET = 0.01; // Offset used for stabilized log of input mel-spectrogram.
    private static final double EXAMPLE_WINDOW_SECONDS = 0.32; // Each example contains NUM_FRAMES 10ms frames
    private static final double EXAMPLE_HOP_SECONDS = 0.32;    // with zero overlap.

    private static final String PCA_EIGEN_VECTORS_NAME = "pca_eigen_vectors";
    private static final String PCA_MEANS_NAME = "pca_means";
    private static final double QUANTIZE_MIN_VAL = -2.0;
    private static final double QUANTIZE_MAX_VAL = +2.0;

    private static final double INIT_STDDEV = 0.01;    // Standard deviation used to initialize weights.
    private static final double LEARNING_RATE = 1e-4;  // Learning rate for the Adam optimizer.
    private static final double ADAM_EPSILON = 1e-8; // Epsilon for the Adam optimizer.

    private static final String INPUT_OP_NAME = "vggish/input_features";
    private static final String INPUT_TENSOR_NAME = INPUT_OP_NAME + ":0";
    private static final String OUTPUT_OP_NAME = "vggish/embedding";
    private static final String OUTPUT_TENSOR_NAME = OUTPUT_OP_NAME + ":0";
    private static final String AUDIO_EMBEDDING_FEATURE_NAME = "audio_embedding";

    private static final int SAMPLING_RATE = 16000;
    /**
     * Take in an array of waveform and return the featured calculated by VGGish.
     * Equivalent to 'main.py'
     * @param waveform: an array of recorded sounds
     * @return an array of floats representing the mfcc features
     */
    public static float[] getFeatures(float[] waveform) {
        // TODO: implement this

        // Convert to [-1.0, +1.0]
        for (int i = 0; i < waveform.length; i++) {
            waveform[i] /= 32768.0;
        }

        return waveformToExamples(waveform, SAMPLING_RATE);
    }

    /**
     * Converter from audio wave into input examples
     * Equivalent to 'vggish_input.waveform_to_examples'
     * @param input:
     * @return
     */
    public static float[] waveformToExamples(float[] input, int samplingRate) {
        // TODO: implement this
        // convert to mono
        // TODO: search what np.mean does?

        // compute log mel spectrogram features
        // TODO: what does log_mel look like after this? should we use external library to replace numpy instead

        // frame features into examples
        double featuresSampleRate = 1.0 / STFT_HOP_LENGTH_SECONDS;
        int exampleWindowLength = (int) Math.round(EXAMPLE_WINDOW_SECONDS * featuresSampleRate);
        int exampleHopeLength = (int) Math.round(EXAMPLE_HOP_SECONDS * featuresSampleRate);

        return null;
    }


}
