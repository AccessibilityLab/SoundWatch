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
}
