package com.wearable.sound.models;

import androidx.annotation.NonNull;

public class SoundPrediction implements Comparable<SoundPrediction> {
    private final String label;     // a more general group label
    private final String subLabel;  // a more specific label
    private final float confidence;

    public SoundPrediction(String label, String subLabel, float confidence) {
        this.label = label;
        this.subLabel = subLabel;
        this.confidence = confidence;
    }

    public String getLabel() {
        return label;
    }

    public String getSubLabel() { return subLabel; }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public int compareTo(SoundPrediction o) {
        if (this.confidence < o.confidence) {
            return -1;
        } else if (this.confidence > o.confidence) {
            return 1;
        }
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return this.label + "_" + this.subLabel + "_" + this.confidence;
    }
}
