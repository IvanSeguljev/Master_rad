package com.ivanseguljev.master_rad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.TextView;

import com.ivanseguljev.master_rad.customview.OverlayView;
import com.ivanseguljev.master_rad.detection_handling.EnchancedVisionDetectionHandler;
import com.ivanseguljev.master_rad.env.BorderedText;
import com.ivanseguljev.master_rad.env.ImageUtils;
import com.ivanseguljev.master_rad.env.LayoutController;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.util.ArrayList;
import java.util.List;

public class EnchancedVision extends CameraActivity implements ImageReader.OnImageAvailableListener {


    private Integer sensorOrientation;

    private TextView textViewInferenceTime;
    private TextView textViewPreviewSize;

    LayoutController layoutController;

    private final int inputImageSize = 320;
    private final float MINIMUM_CONFIDENCE_OD = 0.6f;
    private final String modelFilename = "model.tflite";
    private final String labelsFilename = "model.tflite";
    private final boolean isQuantized = true;

    OverlayView trackingOverlay;
    private Detector apiModel;

    private EnchancedVisionDetectionHandler enchancedVisionDetectionHandler;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private byte[][] yuvBytes = new byte[3][];
    //for process image
    private long timestamp;
    private boolean computingDetection = false;
    private long lastProcessingTimeMs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enchanced_vision);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        layoutController = new LayoutController().init(this);



        textViewInferenceTime = findViewById(R.id.textViewInferenceTime);
        textViewPreviewSize = findViewById(R.id.textViewPreviewSize);
    }



    private void initDetector()
    {
        try {
            apiModel = TFLiteObjectDetectionAPIModel.create(this, modelFilename, labelsFilename, inputImageSize, isQuantized);
            apiModel.setNumThreads(4);
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("Greska u pozivanju detektora");
        }
    }


    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        //initializing object detector
        initDetector();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        cropSize = (previewHeight/4)*3;
        sensorOrientation = rotation - getScreenOrientation();

        //initializing detection handler
        enchancedVisionDetectionHandler = new EnchancedVisionDetectionHandler(this,previewWidth,previewHeight,sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(inputImageSize, inputImageSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        inputImageSize, inputImageSize,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        enchancedVisionDetectionHandler.draw(canvas);
                    }
                });


    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
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

                        enchancedVisionDetectionHandler.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        textViewInferenceTime.setText(lastProcessingTimeMs + " milisekundi");
                                        textViewPreviewSize.setText(previewWidth + "x" + previewHeight);
                                    }
                                });
                    }
                });
    }

}