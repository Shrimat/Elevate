package com.example.elevate;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasySplashScreen config = new EasySplashScreen(this)
                .withFullScreen()
                .withTargetActivity(MapsActivity.class)
                .withSplashTimeOut(2000)
                .withBackgroundColor(Color.WHITE)
                .withAfterLogoText("Elevate")
                .withLogo(R.mipmap.elevate_icon);

        config.getAfterLogoTextView().setTypeface(Typeface.SANS_SERIF);
        config.getAfterLogoTextView().setTextColor(Color.parseColor("#2F5BF4"));

        View easySplashScreen = config.create();
        setContentView(easySplashScreen);
    }
}