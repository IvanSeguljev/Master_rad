package com.ivanseguljev.master_rad.detection_handling;

import android.graphics.Bitmap;

import java.util.List;

public class DetectionsToDisplay {
    public Bitmap lastDetection = null;
    public List<Bitmap> detectionFeed = null;
    public boolean isDetectedStop = false;
    public boolean isDetectedWarningOnSpot =false;
    public boolean isDetectedNoParking =false;
}
