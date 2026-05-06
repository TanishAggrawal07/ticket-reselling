package com.tanish.retix;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Custom Application class for ReTix.
 *
 * Initializes the API client for backend communication.
 * This replaces Firebase initialization with the new REST API backend.
 */
public class ReTixApplication extends Application {

    private static final String PREFS_NAME = "ReTixPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply theme preference - defaults to light mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize API client
        ApiClient.init(this);
    }
}
