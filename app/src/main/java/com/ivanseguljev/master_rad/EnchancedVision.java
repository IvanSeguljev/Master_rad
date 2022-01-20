package com.ivanseguljev.master_rad;

import static android.media.MediaCodec.MetricsConstants.MODE;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.widget.Toast;

import com.ivanseguljev.master_rad.camera.CameraConnectionFragment;
import com.ivanseguljev.master_rad.camera.LegacyCameraConnectionFragment;
import com.ivanseguljev.master_rad.customview.OverlayView;
import com.ivanseguljev.master_rad.env.BorderedText;
import com.ivanseguljev.master_rad.env.ImageUtils;
import com.ivanseguljev.master_rad.env.Logger;
import com.ivanseguljev.master_rad.tracking.MultiBoxTracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class EnchancedVision extends AppCompatActivity implements ImageReader.OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final int SENSOR_ORIENTATION = 90;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private Integer sensorOrientation;

    private final int inputImageSize = 320;
    private final float MINIMUM_CONFIDENCE_OD = 0.5f;
    private final String modelFilename = "model.tflite";
    private final String labelsFilename = "model.tflite";
    private final boolean isQuantized = true;

    private BorderedText borderedText;
    OverlayView trackingOverlay;
    private Detector apiModel;
    private MultiBoxTracker tracker;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    //for on image available
    private Runnable imageConverter;
    private int[] rgbBytes = null;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private Runnable postInferenceCallback;
    //for process image
    private long timestamp;
    private boolean computingDetection = false;
    private long lastProcessingTimeMs;


    private Handler handler;
    private HandlerThread handlerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enchanced_vision);

        requestPermission();
        if (hasCameraPermission()) {
            setFragment();
        }
    }

    private boolean hasCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasCameraPermission()) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        EnchancedVision.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    private void initDetector()
    {
        try {
            apiModel = TFLiteObjectDetectionAPIModel.create(this, modelFilename, labelsFilename, inputImageSize, isQuantized);
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("error ocurred");
        }
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // dont use front facing camera.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;

            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    EnchancedVision.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            R.layout.fragment_tracking,
                            DESIRED_PREVIEW_SIZE);

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        initDetector();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        System.out.println("size: w-"+previewWidth+", h-"+previewHeight);
        sensorOrientation = rotation - getScreenOrientation();
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
                        tracker.draw(canvas);
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }
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

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);
                        float minimumConfidence = MINIMUM_CONFIDENCE_OD;

                        final List<Detector.Recognition> mappedRecognitions =
                                new ArrayList<Detector.Recognition>();

                        for (final Detector.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
//                                        showFrameInfo(previewWidth + "x" + previewHeight);
//                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });
                    }
                });
    }
    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }
    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }
    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }
}