package com.wearable.sound.ui.activity.models;

public class AudioLabel {
    public String label;
    public double confidence;
    public String time;
    public String db;
    public String recordTime;

    public AudioLabel(String label, String confidence, String time, String db, String recordTime) {
        this.label = label;
        this.confidence = Double.parseDouble(confidence);
        this.time = time;
        this.db = db;
        this.recordTime = recordTime;
    }
}
