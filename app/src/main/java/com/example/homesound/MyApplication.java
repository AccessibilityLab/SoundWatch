package com.example.homesound;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    public String SERVER_PORT = "9091";
    private List blockedSounds = new ArrayList();
    private boolean appInForeground = false;

    public List getBlockedSounds() {
        return blockedSounds;
    }

    public void addBlockedSounds(int soundId) {
        this.blockedSounds.add(soundId);
    }

    public void removeBlockedSounds(int soundId) {
        this.blockedSounds.remove(new Integer(soundId));
    }

    public boolean isAppInForeground() {
        return appInForeground;
    }

    public void setAppInForeground(boolean value) {
        this.appInForeground = value;
    }

    public static int getIntegerValueOfSound(String sound){
        int i = 0;
        for (char c : sound.toCharArray())
            i+=(int)c;
        return i;
    }
}