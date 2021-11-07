package com.wearable.sound.models;

import androidx.annotation.NonNull;

public class SoundPrediction implements Comparable<SoundPrediction> {
    private final String label;
    private final float accuracy;

    public SoundPrediction(String label, float accuracy) {
        this.label = label;
        this.accuracy = accuracy;
    }

    public String getLabel() {
        return label;
    }

    public float getAccuracy() {
        return accuracy;
    }

    @Override
    public int compareTo(SoundPrediction o) {
        if (this.accuracy < o.accuracy) {
            return -1;
        } else if (this.accuracy > o.accuracy) {
            return 1;
        }
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return this.label + "_" + this.accuracy;
    }
}
