package com.wearable.sound.utils;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    /**
     * Data layer paths which is the data sent from watch
     */
    public static final String START_ACTIVITY_PATH = "/start-activity";
    public static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String AUDIO_PREDICTION_PATH = "/audio-prediction";
    public static final String SOUND_ENABLE_FROM_PHONE_PATH = "/SOUND_ENABLE_FROM_PHONE_PATH";
    public static final String SEND_CURRENT_BLOCKED_SOUND_PATH = "/SEND_CURRENT_BLOCKED_SOUND_PATH";
    public static final String SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH = "/SEND_ALL_AUDIO_PREDICTIONS_FROM_PHONE_PATH";
    public static final String SEND_FOREGROUND_SERVICE_STATUS_FROM_PHONE_PATH = "/SEND_FOREGROUND_SERVICE_STATUS_FROM_PHONE_PATH";
    public static final String SEND_LISTENING_STATUS_FROM_PHONE_PATH = "/SEND_LISTENING_STATUS_FROM_PHONE_PATH";
    public static final String COUNT_PATH = "/count";

    /**
     *
     */

    public static final String SNOOZE_TIME = "SNOOZE_TIME";
    public static final String SOUND_SNOOZE_FROM_WATCH_PATH = "/SOUND_SNOOZE_FROM_WATCH_PATH";
    public static final String WATCH_CONNECT_STATUS = "/WATCH_CONNECT_STATUS";
    public static final String CONNECTED_HOST_IDS = "CONNECTED_HOST_IDS";
    public static final String SNOOZE_SOUND = "SNOOZE_SOUND";
    public static final String SOUND_ID = "SOUND_ID";
    public static final String SOUND_LABEL = "SOUND_LABEL";
    /**
     *
     */
    public static final String AUDIO_LABEL = "AUDIO_LABEL";
    public static final String FOREGROUND_LABEL = "FOREGROUND_LABEL";
    public static final String WATCH_STATUS_LABEL = "WATCH_STATUS_LABEL";
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final String VOICE_FILE_NAME = "audiorecord.pcm";

    public static final String MODEL_1 = "file:///android_asset/sw_model_1.tflite";
    public static final String MODEL_2 = "file:///android_asset/sw_model_2.tflite";
    public static final String LABEL_FILENAME = "file:///android_asset/labels.txt";

    public static final String MODEL_FILENAME = MODEL_2;
    public static final boolean TEST_MODEL_LATENCY = false;
    public static final boolean TEST_E2E_LATENCY = false;

    public static final String NORMAL_MODE = "NORMAL_MODE";
    public static final String LOW_ACCURACY_FAST_MODE = "LOW_ACCURACY_FAST_MODE";
    public static final String HIGH_ACCURACY_SLOW_MODE = "HIGH_ACCURACY_SLOW_MODE";
    public static final String MODE = NORMAL_MODE;


    public static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
    public static final String TEST_MODEL_LATENCY_SERVER = "http://128.208.49.41:8790";
    public static final String DEFAULT_SERVER = "http://128.208.49.41:8788";

    /**
     * Sound or sound features send configuration
     */
    public static final String RAW_AUDIO_TRANSMISSION = "RAW_AUDIO_TRANSMISSION";
    public static final String AUDIO_FEATURES_TRANSMISSION = "AUDIO_FEATURES_TRANSMISSION";
    public static final String AUDIO_TRANMISSION_STYLE = RAW_AUDIO_TRANSMISSION;

    /**
     * Architecture configurations
     */
    public static final String PHONE_WATCH_ARCHITECTURE = "PHONE_WATCH_ARCHITECTURE";
    public static final String PHONE_WATCH_SERVER_ARCHITECTURE = "PHONE_WATCH_SERVER_ARCHITECTURE";
    public static final String WATCH_ONLY_ARCHITECTURE = "WATCH_ONLY_ARCHITECTURE";
    public static final String WATCH_SERVER_ARCHITECTURE = "WATCH_SERVER_ARCHITECTURE";
    public static final String ARCHITECTURE = PHONE_WATCH_ARCHITECTURE;


    /*
     *  Foreground Service configurations
     * */
    public interface ACTION {
        String MAIN_ACTION = "com.wearable.sound.utils.action.main";
        public static String PREV_ACTION = "com.wearable.sound.utils.action.prev";
        public static String PLAY_ACTION = "com.wearable.sound.utils.action.play";
        public static String NEXT_ACTION = "com.wearable.sound.utils.action.next";
        public static String STARTFOREGROUND_ACTION = "com.wearable.sound.utils.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.wearable.sound.utils.action.stopforeground";
    }
}
