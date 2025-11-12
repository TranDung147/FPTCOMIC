package com.example.fptcomicapp;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final int WELCOME_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_welcome);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            finish();
        }, WELCOME_DELAY);
    }
}
