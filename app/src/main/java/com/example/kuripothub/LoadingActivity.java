package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class LoadingActivity extends AppCompatActivity {

    private static final int LOADING_DURATION = 5000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Hide the action bar for full screen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Load the animated GIF using Glide
        ImageView loadingGif = findViewById(R.id.loadingGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(loadingGif);

        // Start the loading timer
        startLoadingTimer();
    }

    private void startLoadingTimer() {
        // Use Handler to delay the transition to PrefaceActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Navigate to PrefaceActivity after 3 seconds
                Intent intent = new Intent(LoadingActivity.this, PrefaceActivity.class);
                startActivity(intent);
                // Finish this activity so user can't go back to loading screen
                finish();
            }
        }, LOADING_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back button during loading to prevent interruption
        // Do nothing or you can call super.onBackPressed() if you want to allow it
    }
}
