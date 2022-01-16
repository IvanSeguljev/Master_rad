package com.ivanseguljev.master_rad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    ImageButton buttonEnchancedVision;
    ImageButton buttonSignDetection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_main);

        buttonEnchancedVision = findViewById(R.id.btn_enchanced_vision);
        buttonSignDetection = findViewById(R.id.btn_sign_detection);

        //sign detection should lead to view that shows cutouts of detected signs
        buttonSignDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"to be implemented",Toast.LENGTH_SHORT).show();
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