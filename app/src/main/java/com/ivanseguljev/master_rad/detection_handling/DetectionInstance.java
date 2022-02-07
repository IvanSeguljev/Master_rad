package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;

import com.ivanseguljev.master_rad.R;

import org.tensorflow.lite.examples.detection.tflite.Detector;

public class DetectionInstance extends Detector.Recognition {
    private long timestamp;
    public final static String[] DETECTION_LABELS = new String[]{"warning_ahead","mandatory","no_stopping_or_parking","stop","give_road","warning_on_spot"};

    public DetectionInstance(String id,RectF location, float detectionConfidence, String title){
        super(id,title,detectionConfidence,location);
        setTimestamp();
    }


    //gets class name to display depending on title
    public String getDisplayText(Context context){
        String displayText="";

        switch (this.getTitle()){
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
            case "warning_on_spot":
                displayText = context.getResources().getString(R.string.detection_warning_on_spot);
                break;
        }
        return displayText;
    }

    //gets class name to display depending on title
    public int getColor(){
        int color = 0;

        switch (this.getTitle()){
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
            case "warning_on_spot":
                color = Color.BLUE;
                break;
        }
        return color;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
}
