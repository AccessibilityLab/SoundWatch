package com.wearable.sound.models;

import androidx.annotation.NonNull;

public class SoundNotification {
    public String label;
    public boolean isEnabled;
    public boolean isSnoozed;

    public SoundNotification(String label, boolean isEnabled, boolean isSnoozed) {
        this.label = label;
        this.isEnabled = isEnabled;
        this.isSnoozed = isSnoozed;
    }

    @NonNull
    @Override
    public String toString() {
        return "SoundNotification{" +
                "label='" + label + '\'' +
                ", isEnabled=" + isEnabled +
                ", isSnoozed=" + isSnoozed +
                '}';
    }
}
