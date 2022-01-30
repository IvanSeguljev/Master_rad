package com.ivanseguljev.master_rad.detection_handling;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;

import com.ivanseguljev.master_rad.env.ImageUtils;
import com.ivanseguljev.master_rad.env.BorderedText;
import com.ivanseguljev.master_rad.env.Logger;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.LinkedList;
import java.util.List;

//class for marking detections on canvas
public class EnchancedVisionDetectionHandler {
    private static int TEXT_SIZE_DIP = 18;
    private static final float MIN_BOX_SIZE = 16.0f;

    private final Context context;
    private final int frameWidth;
    private final int frameHeight;
    private final int sensorOrientation;
    private Matrix frameToCanvasMatrix;
    private List<DetectionInstance> detectedInstances;
    private final Paint boxPaint = new Paint();
    private final BorderedText borderedText;
    private final Logger logger = new Logger();

    public EnchancedVisionDetectionHandler(Context context, int previewWidth, int previewHeight, int sensorOrientation){
        this.context = context;
        this.frameWidth =previewWidth;
        this.frameHeight =previewHeight;
        this.sensorOrientation = sensorOrientation;
        this.detectedInstances = new LinkedList<>();

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    public synchronized void draw(final Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier =
                Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);
        for (final DetectionInstance recognition : detectedInstances) {
            final RectF trackedPos = new RectF(recognition.location);

            frameToCanvasMatrix.mapRect(trackedPos);
            boxPaint.setColor(recognition.getColor());

            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

            final String labelString =
                    !TextUtils.isEmpty(recognition.getDisplayText(context))
                            ? String.format("%s %.2f", recognition.getDisplayText(context), (100 * recognition.detectionConfidence))
                            : String.format("%.2f", (100 * recognition.detectionConfidence));
            borderedText.drawText(
                    canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);
        }
    }

    public synchronized void trackResults(final List<Detector.Recognition> results, final long timestamp) {
        logger.i("Processing %d results from %d", results.size(), timestamp);
        processResults(results);
    }

    private void processResults(final List<Detector.Recognition> results) {
        final List<Pair<Float, Detector.Recognition>> rectsToTrack = new LinkedList<Pair<Float, Detector.Recognition>>();
        final Matrix rgbFrameToScreen = new Matrix(frameToCanvasMatrix);

        for (final Detector.Recognition result : results) {
            if (result.getLocation() == null) {
                continue;
            }
            final RectF detectionFrameRect = new RectF(result.getLocation());

            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);


            if (detectionFrameRect.width() < MIN_BOX_SIZE || detectionFrameRect.height() < MIN_BOX_SIZE) {
                logger.w("Detektovan premali kvadrat, preskace se! " + detectionFrameRect);
                continue;
            }

            rectsToTrack.add(new Pair<Float, Detector.Recognition>(result.getConfidence(), result));
        }

        detectedInstances.clear();
        if (rectsToTrack.isEmpty()) {
            logger.v("Nista nije detektovano!");
            return;
        }

        for (final Pair<Float, Detector.Recognition> potential : rectsToTrack) {
            final DetectionInstance instance = new DetectionInstance(
                    new RectF(potential.second.getLocation()),
                    potential.first,
                    potential.second.getTitle()
            );
            detectedInstances.add(instance);

        }
    }

}
