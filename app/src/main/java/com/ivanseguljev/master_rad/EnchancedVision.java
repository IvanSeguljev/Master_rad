package com.ivanseguljev.master_rad;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

public class EnchancedVision extends AppCompatActivity {
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final int PERMISSIONS_REQUEST = 1;

    private final int inputImageSize = 320;
    private final String modelFilename = "model.tflite";
    private final String labelsFilename = "model.tflite";
    private final boolean isQuantized = true;

    private Detector apiModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("on create");
        super.onCreate(savedInstanceState);
        try {
            apiModel = TFLiteObjectDetectionAPIModel.create(this, modelFilename, labelsFilename, inputImageSize, isQuantized);
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("error ocurred");
        }
        setContentView(R.layout.activity_enchanced_vision);
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
}