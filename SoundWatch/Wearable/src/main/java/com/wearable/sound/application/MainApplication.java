package com.wearable.sound.application;

import android.app.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application {
    private final List<String> enabledSounds = new ArrayList<>(Arrays.asList(
            "Fire/Smoke Alarm",
            "Door In-Use",
            "Water Running",
            "Knocking",
            "Microwave",
            "Dog Bark",
            "Cat Meow",
            "Car Honk",
            "Vehicle",
            "Crying",
            "Emergency Vehicle",
            "Doorbell",
            "Phone",
            "Cutlery/Silverware",
            "Laughter",
            "Yelling",
            "Bird",
            "Walk/Footsteps",
            "Duck/Goose"
                                                                            ));
    private final List<Integer> blockedSounds = new ArrayList<>();
    private boolean appInForeground = false;

    public List<String> getEnabledSounds() {
        return this.enabledSounds;
    }

    public void addEnabledSound(String sound) {
        if (!this.enabledSounds.contains(sound)) {
            this.enabledSounds.add(sound);
        }
    }

    public void removeEnabledSound(String sound) {
        this.enabledSounds.remove(sound);
    }

    public List<Integer> getBlockedSounds() {
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

    public int getIntegerValueOfSound(String sound) {
        int i = 0;
        for (char c : sound.toCharArray())
            i += c;
        return i;
    }
}