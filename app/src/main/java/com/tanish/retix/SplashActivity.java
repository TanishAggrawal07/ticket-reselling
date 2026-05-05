package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME        = "ReTixPrefs";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";
    private static final String KEY_IS_LOGGED_IN  = "is_logged_in";
    private static final String KEY_DARK_MODE     = "dark_mode_enabled";
    private static final long   SPLASH_DELAY      = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved dark mode preference ONCE at app launch.
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

        // Use FirebaseAuth as the authoritative session check.
        // If the Firebase session is still valid the user is logged in,
        // regardless of what the local SharedPreferences flag says.
        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

        // Keep the local flag in sync so other parts of the app stay consistent
        if (!isLoggedIn && prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();
        }

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
