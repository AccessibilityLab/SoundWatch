package com.wearable.sound;

public class SoundPrediction implements Comparable<SoundPrediction>{
    private String label;
    private float accuracy;

    public SoundPrediction(String label, float accuracy) {
        this.label = label;
        this.accuracy = accuracy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
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
}
