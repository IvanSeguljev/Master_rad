package com.ivanseguljev.master_rad.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;

import com.ivanseguljev.master_rad.R;


public class FlashNotifUtil {
    //stopSignParams
    long stopTimeout = 0;
    boolean isStopShowing;
    long warningOnSpotTimeout=0;
    boolean isWarningOnSpotShowing;
    long noParkingTimeout;
    boolean isNoParkingShowing;
    private View flashNotif;
    private AnimationSet flashAnimation;
    Context context;
    public FlashNotifUtil(Context context, View flashNotif, AnimationSet flashAnimation){
        this.context=context;
        this.flashNotif = flashNotif;
        this.flashAnimation = flashAnimation;
    }
    private void playFulscreenFlash(int animationColor){
        flashNotif.setBackgroundColor(animationColor);
        flashNotif.startAnimation(flashAnimation);
    }
    public void updateStop(boolean isDetected, ImageView imageViewStop){
        if(isDetected){
            if(!isStopShowing) {
                imageViewStop.setBackgroundColor(Color.YELLOW);
                imageViewStop.setImageResource(R.drawable.stop_normal);
                imageViewStop.setAlpha(1f);
                System.out.println("Stop");
                playFulscreenFlash(Color.RED);
            }
            stopTimeout = System.currentTimeMillis()+5000;
            isStopShowing=true;
        }else{
            if(isStopShowing){
                if(stopTimeout<System.currentTimeMillis()) {
                    imageViewStop.setBackgroundColor(Color.TRANSPARENT);
                    imageViewStop.setAlpha(0.2f);
                    imageViewStop.setImageResource(R.drawable.stop_darken);
                    isStopShowing = false;
                }
            }
        }
    }
    public void updateWarningOnSpot(boolean isDetected, ImageView imageViewWarningOnSpot){
        if(isDetected){
            if(!isWarningOnSpotShowing) {
                imageViewWarningOnSpot.setBackgroundColor(Color.YELLOW);
                imageViewWarningOnSpot.setImageResource(R.drawable.pesacki_normal);
                imageViewWarningOnSpot.setAlpha(1f);
                System.out.println("Stop");
                playFulscreenFlash(Color.BLUE);
            }
            warningOnSpotTimeout = System.currentTimeMillis()+5000;
            isWarningOnSpotShowing=true;
        }else{
            if(isWarningOnSpotShowing){
                if(warningOnSpotTimeout<System.currentTimeMillis()) {
                    imageViewWarningOnSpot.setBackgroundColor(Color.TRANSPARENT);
                    imageViewWarningOnSpot.setImageResource(R.drawable.no_parking_darken);
                    imageViewWarningOnSpot.setAlpha(0.2f);
                    isWarningOnSpotShowing = false;
                }
            }
        }
    }public void updateNoParking(boolean isDetected, ImageView imageViewNoParking){
        if(isDetected){
            if(!isNoParkingShowing) {
                imageViewNoParking.setBackgroundColor(Color.YELLOW);
                imageViewNoParking.setImageResource(R.drawable.no_parking_normal);
                imageViewNoParking.setAlpha(1f);
                System.out.println("Stop");
                playFulscreenFlash(Color.MAGENTA);
            }
            noParkingTimeout = System.currentTimeMillis()+5000;
            isNoParkingShowing=true;
        }else{
            if(isNoParkingShowing){
                if(noParkingTimeout<System.currentTimeMillis()) {
                    imageViewNoParking.setBackgroundColor(Color.TRANSPARENT);
                    imageViewNoParking.setImageResource(R.drawable.no_parking_darken);
                    imageViewNoParking.setAlpha(0.2f);
                    isNoParkingShowing = false;
                }
            }
        }
    }

}
