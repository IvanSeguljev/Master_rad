package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import com.ivanseguljev.master_rad.env.Logger;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//handles detections of roadsign detection class
//Instead of real time sign tracking, it gets detections and displays them on photos
public class RoadsignDetectionHandler {
    private Context context;
    private Bitmap frame = null;
    private List<Detector.Recognition> detections;
    private final Logger logger = new Logger();
    private Map<String, DetectionInstance> detectionsToDisplay;
    private int previewWidth;
    private int previewHeight;

    public RoadsignDetectionHandler(Context context, int previewWidth, int previewHeight) {
        this.context = context;
        this.previewHeight = previewHeight;
        this.previewWidth = previewWidth;
        initDetectionsToDisplay();
    }

    private void initDetectionsToDisplay() {
        detectionsToDisplay = new HashMap<>();
        for (String label : DetectionInstance.DETECTION_LABELS) {
            detectionsToDisplay.put(label, null);
        }
    }

    public void setFrame(Bitmap frame) {
        this.frame = frame;
    }

    public synchronized void trackResults(final List<Detector.Recognition> results, final long timestamp) {
        logger.i("Processing %d results from %d", results.size(), timestamp);
        loadResultsIntoMap(results);
    }

    private void loadResultsIntoMap(List<Detector.Recognition> results) {
        for (Detector.Recognition recognition : results) {
            DetectionInstance nextToBeDisplayed = detectionsToDisplay.get(recognition.getTitle());
            if (nextToBeDisplayed == null) {
                detectionsToDisplay.put(recognition.getTitle(), getDetInsFromRecog(recognition));
            } else {
                if (nextToBeDisplayed.getDetectionConfidence() <= recognition.getConfidence())
                    detectionsToDisplay.put(recognition.getTitle(), getDetInsFromRecog(recognition));
            }
        }
    }

    private DetectionInstance getDetInsFromRecog(Detector.Recognition recognition) {
        DetectionInstance res = new DetectionInstance(recognition.getLocation(), recognition.getConfidence(), recognition.getTitle());
        res.setTimestamp(System.currentTimeMillis());
        return res;
    }

    public synchronized void clearResultsMap() {
        detectionsToDisplay.replaceAll((k, v) -> null);
    }

    public synchronized Bitmap getLastDetectionBitmap() {
        DetectionInstance lastDetection = null;
        String[] labelsToInclude = new String[]{"warning_ahead", "mandatory"};
        for (String s : labelsToInclude) {
            DetectionInstance d = detectionsToDisplay.get(s);
            if (d == null)
                continue;
            if (lastDetection == null) {
                lastDetection = detectionsToDisplay.get(s);
            } else {
                if (lastDetection.getDetectionConfidence() < detectionsToDisplay.get(s).getDetectionConfidence()) {
                    lastDetection = detectionsToDisplay.get(s);
                }
            }
        }
        if (lastDetection != null) {
            int x = Math.max(0, (int) lastDetection.getLocation().left);
            int y = Math.max(0, (int) lastDetection.getLocation().top);
            int wcorr = Math.max(0,(int)(x+lastDetection.getLocation().width()-this.frame.getWidth()));
            int hcorr = Math.max(0,(int)(y+lastDetection.getLocation().height()-this.frame.getHeight()));
            System.out.println("rgbw: " + this.frame.getWidth() +" rgbh: " +this.frame.getHeight()+"x: " + (int) lastDetection.getLocation().left + " y: " + (int) lastDetection.getLocation().left + " w: " + (int) (lastDetection.getLocation().width() - 1) + " h: " + (int) (lastDetection.getLocation().height() - 1));
            return Bitmap.createBitmap(
                    frame,
                    x,
                    y,
                    (int) (lastDetection.getLocation().width()-wcorr),
                    (int) (lastDetection.getLocation().height()-hcorr)
            );
        }
        return null;
    }
}
