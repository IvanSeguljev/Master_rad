package com.ivanseguljev.master_rad;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivanseguljev.master_rad.detection_handling.DetectionsToDisplay;
import com.ivanseguljev.master_rad.detection_handling.RoadsignDetectionHandler;
import com.ivanseguljev.master_rad.env.BorderedText;
import com.ivanseguljev.master_rad.env.ImageUtils;
import com.ivanseguljev.master_rad.env.LayoutController;
import com.ivanseguljev.master_rad.util.DetectionFeedAdapter;
import com.ivanseguljev.master_rad.util.FlashNotifUtil;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.util.ArrayList;
import java.util.List;

public class RoadsignDetection extends CameraActivity {
    LayoutController layoutController;



    private Integer sensorOrientation;

    private TextView textViewInferenceTime;
    private TextView textViewPreviewSize;
    private View flashNotif;
    private ImageView imageViewStopDetected;
    private ImageView imageViewWarningOnSpotDetected;
    private ImageView imageViewNoParkingDetected;

    private final int inputImageSize = 320;
    private final float MINIMUM_CONFIDENCE_OD = 0.6f;
    private final String modelFilename = "model.tflite";
    private final String labelsFilename = "model.tflite";
    private final boolean isQuantized = true;

    private BorderedText borderedText;
    private Detector apiModel;

    private RoadsignDetectionHandler roadsignDetectionHandler;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Runnable postInferenceCallback;
    //for process image
    private long timestamp;
    private boolean computingDetection = false;
    private long lastProcessingTimeMs;


    private ImageView imageViewLastDetected;
    private FlashNotifUtil flashNotifUtil;
    private RecyclerView recyclerViewDetectionFeed;
    private DetectionFeedAdapter detectionFeedAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roadsign_detection);
        layoutController = new LayoutController().init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Getting UI components
        flashNotif = findViewById(R.id.flash_notif);
        imageViewLastDetected = findViewById(R.id.imageview_last_detected);
        textViewInferenceTime = findViewById(R.id.textViewInferenceTime);
        imageViewStopDetected = findViewById(R.id.imageview_stop_detected);
        imageViewWarningOnSpotDetected = findViewById(R.id.imageview_warning_on_spot_detected);
        imageViewNoParkingDetected = findViewById(R.id.imageview_no_parking_detected);
        recyclerViewDetectionFeed = findViewById(R.id.recycler_detection_feed);
        //setting recycler view things
        detectionFeedAdapter = new DetectionFeedAdapter();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerViewDetectionFeed.setLayoutManager(mLayoutManager);
        recyclerViewDetectionFeed.setItemAnimator(new DefaultItemAnimator());
        recyclerViewDetectionFeed.setAdapter(detectionFeedAdapter);
        //Flash animation
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(200);
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(200);
        fadeOut.setDuration(1000);
        AnimationSet flashAnim = new AnimationSet(false);
        flashAnim.addAnimation(fadeIn);
        flashAnim.addAnimation(fadeOut);

        flashNotifUtil = new FlashNotifUtil(this,flashNotif,flashAnim);

    }


    private void initDetector() {
        try {
            apiModel = TFLiteObjectDetectionAPIModel.create(this, modelFilename, labelsFilename, inputImageSize, isQuantized);
            apiModel.setNumThreads(4);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Greska u pozivanju detektora");
        }
    }



    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        initDetector();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        cropSize = (previewHeight/4)*3;
        sensorOrientation = rotation - getScreenOrientation();


        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(inputImageSize, inputImageSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        cropSize, cropSize,
                        inputImageSize, inputImageSize,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        //initializingDetectionHandler
        roadsignDetectionHandler = new RoadsignDetectionHandler(this,previewWidth,previewHeight);


    }
    @Override
    protected void processImage() {
        ++timestamp;
        final long imagenum = timestamp;

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + imagenum + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        roadsignDetectionHandler.setFrame(rgbFrameBitmap);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(Bitmap.createBitmap(rgbFrameBitmap,0,0,cropSize,cropSize), frameToCropTransform, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + imagenum);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Detector.Recognition> results = apiModel.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;


                        final List<Detector.Recognition> mappedRecognitions =
                                new ArrayList<Detector.Recognition>();

                        for (final Detector.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_OD) {

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }
                        roadsignDetectionHandler.trackResults(mappedRecognitions, imagenum);

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        show_detections_on_UI(roadsignDetectionHandler.getDetectionsToDisplay());
                                        roadsignDetectionHandler.clearResultsMap();
                                        textViewInferenceTime.setText(lastProcessingTimeMs + " milisekundi");
//                                        textViewPreviewSize.setText(previewWidth + "x" + previewHeight);
                                    }
                                });
                    }
                });
    }
    private void show_detections_on_UI(DetectionsToDisplay detectionsToDisplay){
        //show last detected item
        if (detectionsToDisplay.lastDetection != null) {
            imageViewLastDetected.setImageBitmap(detectionsToDisplay.lastDetection);
            imageViewLastDetected.postInvalidate();
        }
        for(Bitmap detection:detectionsToDisplay.detectionFeed){
            detectionFeedAdapter.addDetection(detection);
        }
        //update detection feed
        flashNotifUtil.updateStop(detectionsToDisplay.isDetectedStop,imageViewStopDetected);
        flashNotifUtil.updateWarningOnSpot(detectionsToDisplay.isDetectedWarningOnSpot,imageViewWarningOnSpotDetected);
        flashNotifUtil.updateNoParking(detectionsToDisplay.isDetectedNoParking,imageViewNoParkingDetected);

    }

}