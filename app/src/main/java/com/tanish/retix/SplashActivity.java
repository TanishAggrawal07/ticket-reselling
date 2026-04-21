package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME        = "ReTixPrefs";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";
    private static final String KEY_IS_LOGGED_IN  = "is_logged_in";
    private static final String KEY_DARK_MODE     = "dark_mode_enabled";
    private static final long   SPLASH_DELAY      = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved dark mode preference ONCE at app launch.
        // All other activities inherit this setting automatically — they must
        // NOT call setDefaultNightMode() themselves, as that triggers recreation
        // loops and crashes.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES
                           : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DELAY);
    }

    private void navigateToNextScreen() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(KEY_IS_FIRST_TIME, true);
        boolean isLoggedIn  = prefs.getBoolean(KEY_IS_LOGGED_IN, false);

        Intent intent;
        if (isFirstTime) {
            intent = new Intent(this, OnboardingActivity.class);
        } else if (isLoggedIn) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
