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
        String STARTFOREGROUND_ACTION = "com.wearable.sound.utils.action.startforeground";
        String STOPFOREGROUND_ACTION = "com.wearable.sound.utils.action.stopforeground";
    }

    /**
     * 30 sounds: 11 high accuracy + 19 low accuracy
     */
    public static final String SPEECH = "Speech";
    public static final String KNOCKING = "Knocking";
    public static final String DOG_BARK = "Dog Bark";
    public static final String CAT_MEOW= "Cat Meow";
    public static final String VEHICLE= "Vehicle";
    // for now, mapping new label "Fire Alarm" to replace this
    public static final String FIRE_SMOKE_ALARM = "Fire/Smoke Alarm";
//    public static final String FIRE_SMOKE_ALARM = "Fire Alarm";
    public static final String WATER_RUNNING = "Water Running";
    public static final String BABY_CRY = "Baby Cry";
    public static final String DOOR_IN_USE = "Door In-Use";
    public static final String CAR_HONK= "Car Honk";
    public static final String APPLIANCES= "Appliances";

    public static final String SHAVER= "Electric Shaver/Electric Razor";
    public static final String TOOTHBRUSH= "Toothbrush";
    //    public static final String BLENDER = "Blender";
//    public static final String DISHWASHER = "Dishwasher";
    public static final String DOORBELL = "Doorbell";
    public static final String TOILET_FLUSH = "Toilet Flush";
    public static final String HAIR_DRYER= "Hair Dryer";
    public static final String LAUGHING= "Laughter";
    public static final String SNORING= "Snoring";
    public static final String HAMMERING= "Hammering";
    public static final String SAWING= "Sawing";
    public static final String ALARM_CLOCK= "Alarm Clock";
    public static final String UTENSILS_AND_CUTLERY= "Cutlery/Silverware";
    public static final String COUGHING = "Cough";
    public static final String TYPING = "Typing";
    //    public static final String PHONE_RING = "Phone Ring";
    public static final String DRILL = "Drill";
    public static final String VACUUM = "Vacuum Cleaner";
    public static final String CHOPPING = "Chopping (Food)";
}
