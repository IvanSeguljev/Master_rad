package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;

import com.ivanseguljev.master_rad.R;

public class DetectionInstance {
    public RectF location;
    public float detectionConfidence;
    private String title;
    private long timestamp;
    public final static String[] DETECTION_LABELS = new String[]{"warning_ahead","mandatory","no_stopping_or_parking","stop","give_road"};

    public DetectionInstance(RectF location, float detectionConfidence, String title){
        this.title = title;
        this.detectionConfidence = detectionConfidence;
        this.location = location;
    }


    //gets class name to display depending on title
    public String getDisplayText(Context context){
        String displayText="";

        switch (title){
            case "warning_ahead":
                displayText = context.getResources().getString(R.string.detection_warning_ahead);
                break;
            case "mandatory":
                displayText = context.getResources().getString(R.string.detection_mandatory);
                break;
            case "no_stopping_or_parking":
                displayText = context.getResources().getString(R.string.detection_no_stopping_or_parking);
                break;
            case "stop":
                displayText = context.getResources().getString(R.string.detection_stop);
                break;
            case "give_road":
                displayText = context.getResources().getString(R.string.detection_give_road);
                break;
        }
        return displayText;
    }

    //gets class name to display depending on title
    public int getColor(){
        int color = 0;

        switch (title){
            case "warning_ahead":
                color = Color.YELLOW;
                break;
            case "mandatory":
                color = Color.WHITE;
                break;
            case "no_stopping_or_parking":
                color = Color.GREEN;
                break;
            case "stop":
                color = Color.RED;
                break;
            case "give_road":
                color = Color.GRAY;
                break;
        }
        return color;
    }
    public RectF getLocation() {
        return location;
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    public float getDetectionConfidence() {
        return detectionConfidence;
    }

    public void setDetectionConfidence(float detectionConfidence) {
        this.detectionConfidence = detectionConfidence;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
