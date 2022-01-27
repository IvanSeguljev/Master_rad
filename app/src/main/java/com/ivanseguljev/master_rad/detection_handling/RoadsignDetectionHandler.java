package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import com.ivanseguljev.master_rad.env.Logger;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//handles detections of roadsign detection class
//Instead of real time sign tracking, it gets detections and displays them on photos
public class RoadsignDetectionHandler {
    private Context context;
    private Bitmap frame = null;
    private List<Detector.Recognition> detections;
    private final Logger logger = new Logger();
    private  Map<String, DetectionInstance> detectionsToDisplay;
    private  Map<String,Long> lastDetectionTimes;
    private int timeoutPeriodMs = 2000;
    private int previewWidth;
    private int previewHeight;

    public RoadsignDetectionHandler(Context context, int previewWidth, int previewHeight) {
        this.context = context;
        this.previewHeight = previewHeight;
        this.previewWidth = previewWidth;
        initMaps();
    }

    private void initMaps() {
        detectionsToDisplay = new HashMap<>();
        for (String label : DetectionInstance.DETECTION_LABELS) {
            detectionsToDisplay.put(label, null);
        }
        lastDetectionTimes = new HashMap<>();
        for (String label : DetectionInstance.DETECTION_LABELS) {
            lastDetectionTimes.put(label, (long)0);
        }
    }

    public void setFrame(Bitmap frame) {
        this.frame = frame;
    }

    public synchronized void trackResults(final List<Detector.Recognition> results, final long timestamp) {
        logger.i("Processing %d results from %d", results.size(), timestamp);
        loadResultsIntoMap(results);
    }
    private boolean readyToDisplayLabel(DetectionInstance detectionInstance){
        return (detectionInstance.getTimestamp()- lastDetectionTimes.get(detectionInstance.getTitle()))>timeoutPeriodMs;
    }
    private void putRecognitionInMap(Detector.Recognition recognition){
        DetectionInstance instance = getDetInsFromRecog(recognition);
        if(readyToDisplayLabel(instance))
            detectionsToDisplay.put(recognition.getTitle(), instance);
    }
    //loads detection recognitions into map. Only highest confidence results of each class will be loaded.
    private void loadResultsIntoMap(List<Detector.Recognition> results) {
        for (Detector.Recognition recognition : results) {
            DetectionInstance nextToBeDisplayed = detectionsToDisplay.get(recognition.getTitle());
            if (nextToBeDisplayed == null)   {
                putRecognitionInMap(recognition);
            } else {
                if (nextToBeDisplayed.getDetectionConfidence() <= recognition.getConfidence())
                    putRecognitionInMap(recognition);
            }
        }
    }
    //gets detection instance from recognition.
    private DetectionInstance getDetInsFromRecog(Detector.Recognition recognition) {
        DetectionInstance res = new DetectionInstance(recognition.getLocation(), recognition.getConfidence(), recognition.getTitle());
        return res;
    }

    public void clearResultsMap() {
        detectionsToDisplay.replaceAll((k, v) -> null);
    }

    private List<DetectionInstance> getDisplayableDetections(){
        String[] labelsToInclude = new String[]{"warning_ahead", "mandatory"};
        ArrayList<DetectionInstance> toDisplay = new ArrayList<>();
        for (String s : labelsToInclude) {
            DetectionInstance pom = detectionsToDisplay.get(s);
            if(pom!=null)
                toDisplay.add(pom);
        }
        return toDisplay;
    }

    private DetectionInstance calculateLastDetection(){
        DetectionInstance lastDetection = null;

        for (DetectionInstance s : getDisplayableDetections()) {
            if (lastDetection == null) {
                lastDetection = s;
            } else {
                if (lastDetection.getDetectionConfidence() < s.getDetectionConfidence()) {
                    lastDetection = s;
                }
            }
        }
        return lastDetection;
    }
    private Bitmap getBitmapForDetection(DetectionInstance detectionInstance){
        if(detectionInstance == null)
            return null;
        int x = Math.max(0, (int) detectionInstance.getLocation().left);
        int y = Math.max(0, (int) detectionInstance.getLocation().top);
        int wcorr = Math.max(0,(int)(x+detectionInstance.getLocation().width()-this.frame.getWidth()));
        int hcorr = Math.max(0,(int)(y+detectionInstance.getLocation().height()-this.frame.getHeight()));
        return Bitmap.createBitmap(
                frame,
                x,
                y,
                (int) (detectionInstance.getLocation().width()-wcorr),
                (int) (detectionInstance.getLocation().height()-hcorr)
        );
    }
    private void updateTimestamps(){
        for(String s:detectionsToDisplay.keySet()){
            if(detectionsToDisplay.get(s)!=null){
                lastDetectionTimes.put(s, detectionsToDisplay.get(s).getTimestamp());
            }
        }
    }

    public synchronized DetectionsToDisplay getDetectionsToDisplay(){
        DetectionsToDisplay detectionsToDisplay = new DetectionsToDisplay();
        detectionsToDisplay.detectionFeed = getDisplayableDetections().stream().map(x->{return getBitmapForDetection(x);}).collect(Collectors.toList());
        detectionsToDisplay.lastDetection = getBitmapForDetection(calculateLastDetection());
        if(this.detectionsToDisplay.get("stop")!=null||this.detectionsToDisplay.get("give_road")!=null)
            detectionsToDisplay.isDetectedStop = true;
        if(this.detectionsToDisplay.get("warning_on_spot")!=null)
            detectionsToDisplay.isDetectedWarningOnSpot = true;
        if(this.detectionsToDisplay.get("no_stopping_or_parking")!=null)
            detectionsToDisplay.isDetectedNoParking=true;
        updateTimestamps();
        clearResultsMap();

        return detectionsToDisplay;

    }
}
