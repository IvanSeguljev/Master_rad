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

import com.ivanseguljev.master_rad.EnchancedVision;
import com.ivanseguljev.master_rad.R;

public class LayoutController {
    ImageView imageViewHamburger;
    DrawerLayout drawerLayout;
    ImageView imageViewDrawerClose;
    //using layout as a button so it looks nice :)
    LinearLayout buttonRoadsignDetection;
    LinearLayout buttonEnchancedVision;
    Context context;

    public LayoutController init(Context context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        imageViewHamburger = ((Activity) context).findViewById(R.id.imageViewHamburger);
        drawerLayout = ((Activity) context).findViewById(R.id.drawer_layout);
        imageViewDrawerClose = ((Activity) context).findViewById(R.id.imageViewDrawerClose);
        buttonRoadsignDetection = ((Activity) context).findViewById(R.id.layout_roadsign_detection);
        buttonEnchancedVision = ((Activity) context).findViewById(R.id.layout_computer_vision);
        System.out.println(context.getClass());

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
        buttonRoadsignDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                Toast.makeText(context.getApplicationContext(), "to be implemented", Toast.LENGTH_SHORT).show();
            }
        });
        if (!EnchancedVision.class.equals(context.getClass())) {
            buttonEnchancedVision.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                    Intent enchancedVisionIntent = new Intent(context, EnchancedVision.class);
                    context.startActivity(enchancedVisionIntent);
                }
            });
        }else {
            buttonEnchancedVision.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                    Toast.makeText(context.getApplicationContext(), "Trenutno na toj aktivnosti", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return this;
    }
}