package com.ivanseguljev.master_rad.env;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ivanseguljev.master_rad.AboutActivity;
import com.ivanseguljev.master_rad.EnchancedVisionActivity;
import com.ivanseguljev.master_rad.MainActivity;
import com.ivanseguljev.master_rad.R;
import com.ivanseguljev.master_rad.RoadsignDetectionActivity;

public class LayoutController {
    ImageView imageViewHamburger;
    DrawerLayout drawerLayout;
    ImageView imageViewDrawerClose;
    //using layout as a button so it looks nice :)
    LinearLayout buttonRoadsignDetection;
    LinearLayout buttonEnchancedVision;
    LinearLayout buttonHome;
    LinearLayout buttonAbout;
    Context context;

    public LayoutController init(Context context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        imageViewHamburger = ((Activity) context).findViewById(R.id.imageViewHamburger);
        drawerLayout = ((Activity) context).findViewById(R.id.drawer_layout);
        imageViewDrawerClose = ((Activity) context).findViewById(R.id.imageViewDrawerClose);
        //navigation buttons
        buttonRoadsignDetection = ((Activity) context).findViewById(R.id.layout_roadsign_detection);
        buttonEnchancedVision = ((Activity) context).findViewById(R.id.layout_computer_vision);
        buttonHome = ((Activity) context).findViewById(R.id.layout_home);
        buttonAbout = ((Activity) context).findViewById(R.id.layout_about);
        System.out.println(context.getClass());
        //Hamburger button for opening and closing nav menu
        imageViewHamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });
        imageViewDrawerClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
        //Roadsign detection
        buttonRoadsignDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                if (!RoadsignDetectionActivity.class.equals(context.getClass())) {
                    Intent roadsignDetectionIntent = new Intent(context, RoadsignDetectionActivity.class);
                    context.startActivity(roadsignDetectionIntent);
                } else {
                    Toast.makeText(context.getApplicationContext(), "Trenutno na toj aktivnosti", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //Enchanced vision
        buttonEnchancedVision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                if (!EnchancedVisionActivity.class.equals(context.getClass())) {
                    Intent enchancedVisionIntent = new Intent(context, EnchancedVisionActivity.class);
                    context.startActivity(enchancedVisionIntent);
                } else {
                    Toast.makeText(context.getApplicationContext(), "Trenutno na toj aktivnosti", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //Home
        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                if (!MainActivity.class.equals(context.getClass())) {
                    Intent homeIntent = new Intent(context, MainActivity.class);
                    context.startActivity(homeIntent);
                } else {
                    Toast.makeText(context.getApplicationContext(), "Trenutno na toj aktivnosti", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //About
        buttonAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                if (!AboutActivity.class.equals(context.getClass())) {
                    Intent aboutIntent = new Intent(context, AboutActivity.class);
                    context.startActivity(aboutIntent);
                } else {
                    Toast.makeText(context.getApplicationContext(), "Trenutno na toj aktivnosti", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return this;
    }
}