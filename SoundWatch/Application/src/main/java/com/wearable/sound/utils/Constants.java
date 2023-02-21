package com.wearable.sound.utils;

import com.wearable.sound.BuildConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Constants {

    public static final boolean DEBUG_LOG = BuildConfig.DEBUG;

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
     * Sounds Labels.
     */
    // 10 main sounds (from old model)
    public static final String KNOCKING = "Knocking";
    public static final String DOG_BARK = "Dog Bark";
    public static final String CAT_MEOW = "Cat Meow";
    public static final String VEHICLE = "Vehicle";
    public static final String FIRE_SMOKE_ALARM = "Fire/Smoke Alarm";
    public static final String WATER_RUNNING = "Water Running";
    public static final String CRYING = "Crying";
    public static final String DOOR_IN_USE = "Door In-Use";
    public static final String CAR_HONK = "Car Honk";
    public static final String MICROWAVE = "Microwave";
    // Additional sounds
    public static final String LAUGHTER = "Laughter";
    public static final String EMERGENCY_VEHICLE = "Emergency Vehicle";
    public static final String BIRD = "Bird";
    public static final String WALK_FOOTSTEPS = "Walk/Footsteps";
    public static final String TELEPHONE = "Phone";
    public static final String DUCK_GOOSE = "Duck/Goose";
    public static final String DOORBELL = "Doorbell";
    public static final String CUTLERY_SILVERWARE = "Cutlery/Silverware";
    public static final String YELLING = "Yelling";
    // Mapping each group label above to specific labels output by the model (e.g labels.txt)
    // E.g: Car Honk and Car Alarm predictions will be grouped into Car Honk
    public static Map<String, Set<String>> groupToLabels;
    static {
        groupToLabels = new HashMap<>();
        groupToLabels.put(KNOCKING,
                new HashSet<>(Arrays.asList("Knocking")));
        groupToLabels.put(DOG_BARK,
                new HashSet<>(Arrays.asList("Dog Bark", "Dog")));
        groupToLabels.put(CAT_MEOW,
                new HashSet<>(Arrays.asList("Cat", "Purr", "Cat Meow")));
        groupToLabels.put(VEHICLE,
                new HashSet<>(Arrays.asList("Vehicle", "Car Passing By", "Car", "Motor Vehicle (Road)")));
        groupToLabels.put(FIRE_SMOKE_ALARM,
                new HashSet<>(Arrays.asList("Fire Alarm", "Smoke Detector/Smoke Alarm")));
        groupToLabels.put(WATER_RUNNING,
                new HashSet<>(Arrays.asList("Water", "Water Tap/Faucet", "Sink (Filling Or Washing)", "Bathtub (Filling Or Washing)", "Liquid")));
        groupToLabels.put(CRYING,
                new HashSet<>(Arrays.asList("Baby Cry", "Crying/Sobbing")));
        groupToLabels.put(DOOR_IN_USE,
                new HashSet<>(Arrays.asList("Door In-Use", "Sliding Door")));
        groupToLabels.put(CAR_HONK,
                new HashSet<>(Arrays.asList("Car Honk", "Car Alarm")));
        groupToLabels.put(MICROWAVE,
                new HashSet<>(Arrays.asList("Microwave", "Beep/Bleep")));
        groupToLabels.put(LAUGHTER,
                new HashSet<>(Arrays.asList("Laughter", "Baby Laughter", "Belly Laugh")));
        groupToLabels.put(EMERGENCY_VEHICLE,
                new HashSet<>(Arrays.asList("Emergency Vehicle", "Police Car (Siren)", "Ambulance (Siren)", "Fire Engine/Fire Truck (Siren)")));
        groupToLabels.put(BIRD,
                new HashSet<>(Arrays.asList("Bird", "Bird Vocalization/Bird Call/Bird Song", "Chirp/Tweet", "Squawk")));
        groupToLabels.put(DUCK_GOOSE,
                new HashSet<>(Arrays.asList("Duck", "Quack", "Goose", "Honk")));
        groupToLabels.put(WALK_FOOTSTEPS,
                new HashSet<>(Arrays.asList("Walk/Footsteps")));
        groupToLabels.put(TELEPHONE,
                new HashSet<>(Arrays.asList("Telephone", "Telephone Bell Ringing", "Ringtone", "Telephone Dialing/Dtmf", "Dial Tone")));
        groupToLabels.put(DOORBELL,
                new HashSet<>(Arrays.asList("Doorbell", "Ding-Dong")));
        groupToLabels.put(CUTLERY_SILVERWARE,
                new HashSet<>(Arrays.asList("Cutlery/Silverware")));
        groupToLabels.put(YELLING,
                new HashSet<>(Arrays.asList("Shout", "Yell", "Battle Cry", "Children Shouting", "Screaming")));
    }

    ;

    /**
     * For FS Logging
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
