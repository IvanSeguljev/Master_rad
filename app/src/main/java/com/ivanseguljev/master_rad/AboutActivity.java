package com.ivanseguljev.master_rad;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.ivanseguljev.master_rad.env.LayoutController;

public class AboutActivity extends AppCompatActivity {
    LayoutController layoutController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        layoutController = new LayoutController().init(this);
    }
}