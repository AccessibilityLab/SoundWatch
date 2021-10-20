package com.wearable.sound.application;

import android.app.Application;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application {
    public List<String> enabledSounds = new ArrayList<>(Arrays.asList(
            "Fire/Smoke Alarm",
            "Door In-Use",
            "Water Running",
            "Knocking",
            "Microwave",
            "Dog Bark",
            "Cat Meow",
            "Car Honk",
            "Vehicle",
            "Baby Cry"
    ));

    private final List<Integer> blockedSounds = new ArrayList<>();
    private boolean appInForeground = false;

    public List<Integer> getBlockedSounds() {
        return blockedSounds;
    }

    public void addBlockedSounds(int soundId) {
        this.blockedSounds.add(soundId);
    }

    public void removeBlockedSounds(int soundId) {
        if (blockedSounds.contains(soundId)) {
            this.blockedSounds.remove(Integer.valueOf(soundId));
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
        this.enabledSounds.remove(sound);
    }

    public static int getIntegerValueOfSound(String sound){
        int i = 0;
        for (char c : sound.toCharArray())
            i += c;
        return i;
    }
}