package com.wearable.sound.utils;

public class Constants {
    /*
     *  Foreground Service configurations
     * */
    public interface ACTION {
        String MAIN_ACTION = "com.wearable.sound.utils.action.main";
        String PREV_ACTION = "com.wearable.sound.utils.action.prev";
        String PLAY_ACTION = "com.wearable.sound.utils.action.play";
        String NEXT_ACTION = "com.wearable.sound.utils.action.next";
        String START_FOREGROUND_ACTION = "com.wearable.sound.utils.action.startforeground";
        String STOP_FOREGROUND_ACTION = "com.wearable.sound.utils.action.stopforeground";
    }

    /**
     * Sound or sound features send configuration
     */
    public static final String RAW_AUDIO_TRANSMISSION = "RAW_AUDIO_TRANSMISSION";
    public static final String AUDIO_FEATURES_TRANSMISSION = "AUDIO_FEATURES_TRANSMISSION";
    public static final String AUDIO_TRANSMISSION_STYLE = RAW_AUDIO_TRANSMISSION;

    /**
     * Architecture configurations
     */
    public static final String PHONE_WATCH_ARCHITECTURE = "PHONE_WATCH_ARCHITECTURE";
    public static final String PHONE_WATCH_SERVER_ARCHITECTURE = "PHONE_WATCH_SERVER_ARCHITECTURE";
    public static final String WATCH_ONLY_ARCHITECTURE = "WATCH_ONLY_ARCHITECTURE";
    public static final String WATCH_SERVER_ARCHITECTURE = "WATCH_SERVER_ARCHITECTURE";
    public static final String ARCHITECTURE = PHONE_WATCH_ARCHITECTURE;

    /**
     * Phone Watch Architecture configuration ONLY!!!
     * [ EXPERIMENTAL ]
     */
    public static final boolean PREDICT_MULTIPLE_SOUNDS = true;

    /**
     * 10 main sounds (new model)
     */
    public static final String KNOCKING = "Knocking";
    public static final String DOG_BARK = "Dog Bark";
    public static final String CAT_MEOW= "Cat Meow";
    public static final String VEHICLE= "Vehicle";
    public static final String FIRE_SMOKE_ALARM = "Fire/Smoke Alarm";
    public static final String WATER_RUNNING = "Water Running";
    public static final String BABY_CRY = "Baby Cry";
    public static final String DOOR_IN_USE = "Door In-Use";
    public static final String CAR_HONK= "Car Honk";
    public static final String MICROWAVE= "Microwave";

    /**
     * For Firebase Analytics
     */
    // Event:
    public static final String PREDICTION_EVENT = "prediction_event";
    public static final String SNOOZE_EVENT = "snooze_event";
    public static final String CHECKBOX_EVENT = "checkbox_event";

    // Param:
    public static final String ALL_SOUNDS_PARAM = "all_sounds_status";
    public static final String SINGLE_SOUND_PARAM = "sound_label";
    public static final String SOUND_PREDICTION_MESSAGE = "sound_prediction_message";
    public static final String SNOOZE_PARAM = "is_snooze";
    public static final String ENABLE_PARAM = "is_enable";
    public static final String SLEEP_MODE_PARAM = "is_sleep_mode_on";
    public static final String DBLEVEL_PARAM = "db_level";
}
