package com.wearable.sound.ui.activity.models;

public class SoundNotification {
    public String label;
    public boolean isEnabled;
    public boolean isSnoozed;

    public SoundNotification(String label, boolean isEnabled, boolean isSnoozed) {
        this.label = label;
        this.isEnabled = isEnabled;
        this.isSnoozed = isSnoozed;
    }

}
