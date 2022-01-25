package com.ivanseguljev.master_rad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ivanseguljev.master_rad.env.LayoutController;

public class MainActivity extends AppCompatActivity {
    ImageButton buttonEnchancedVision;
    ImageButton buttonSignDetection;
    LayoutController layoutController;

    private boolean isHamburgerOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layoutController = new LayoutController().init(this);

        buttonEnchancedVision = findViewById(R.id.btn_enchanced_vision);
        buttonSignDetection = findViewById(R.id.btn_sign_detection);

        //sign detection should lead to view that shows cutouts of detected signs
        buttonSignDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent roadsignDetectionIntent = new Intent(MainActivity.this, RoadsignDetection.class);
                MainActivity.this.startActivity(roadsignDetectionIntent);
            }
        });

        //enchanced vision should lead to real time inference
        buttonEnchancedVision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent enchancedVisionIntent = new Intent(MainActivity.this,EnchancedVision.class);
                MainActivity.this.startActivity(enchancedVisionIntent);
            }
        });
    }
}