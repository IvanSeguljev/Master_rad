package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;

import com.ivanseguljev.master_rad.R;

public class DetectionInstance {
    public RectF location;
    public float detectionConfidence;
    private String title;

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
}
