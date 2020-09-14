package com.wearable.sound.application;

import android.app.Application;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application {
    public ArrayList<String> enabledSounds = new ArrayList<>(Arrays.asList("Fire/Smoke Alarm",
            "Speech",
            "Door In-Use",
            "Water Running",
            "Knocking",
            "Microwave",
            "Dog Bark",
            "Cat Meow",
            "Car Honk",
            "Vehicle",
            "Baby Cry"));

//    public ArrayList<String> enabledSounds = new ArrayList<>(Arrays.asList(new String[] {
//        "Dog Bark",
//        "Drill",
//        "Fire/Smoke Alarm",
//        "Phone Ring",
//        "Speech",
//        "Vacuum",
//        "Baby Cry",
//        "Chopping",
//        "Coughing",
//        "Door In-Use",
//        "Water Running",
//        "Knocking",
//        "Microwave",
//        "Shaver",
//        "Toothbrush",
//        "Blender",
//        "Dishwasher",
//        "Doorbell",
//        "Toilet Flush",
//        "Hair Dryer",
//        "Laughing",
//        "Snoring",
//        "Typing",
//        "Hammering",
//        "Car Honk",
//        "Vehicle",
//        "Saw",
//        "Cat Meow",
//        "Alarm Clock",
//        "Utensils and Cutlery"
//    }));
    private List blockedSounds = new ArrayList();
    private boolean appInForeground = false;

    public List getBlockedSounds() {
        return blockedSounds;
    }

    public void addBlockedSounds(int soundId) {
        this.blockedSounds.add(soundId);
    }

    public void removeBlockedSounds(int soundId) {
        if (blockedSounds.contains(soundId)) {
            this.blockedSounds.remove(new Integer(soundId));
        }
    }

    public boolean isAppInForeground() {
        return appInForeground;
    }

    public void setAppInForeground(boolean value) {
        this.appInForeground = value;
    }

    public void addEnabledSound(String sound) {
        if (!this.enabledSounds.contains(sound)) {
            this.enabledSounds.add(sound);
        }
    }

    public void removeEnabledSound(String sound) {
        if (this.enabledSounds.contains(sound)) {
            this.enabledSounds.remove(sound);
        }
    }

    public static int getIntegerValueOfSound(String sound){
        int i = 0;
        for (char c : sound.toCharArray())
            i+=(int)c;
        return i;
    }
}