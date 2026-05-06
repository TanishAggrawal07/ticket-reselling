package com.tanish.retix;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SettingsActivity - App settings and logout.
 * Uses the new API-based authentication.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ImageButton btnBack;
    private View rowEditProfile;
    private View rowLogout;
    private View rowAboutApp;
    private SwitchMaterial switchDarkMode;
    private TextView tvDarkModeStatus;

    private boolean isInitialisingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        setupDarkModeToggle();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rowEditProfile = findViewById(R.id.card_edit_profile);
        rowLogout = findViewById(R.id.card_logout);
        rowAboutApp = findViewById(R.id.card_about_app);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        tvDarkModeStatus = findViewById(R.id.tv_dark_mode_status);
    }

    private void setupToolbar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupDarkModeToggle() {
        if (switchDarkMode == null || tvDarkModeStatus == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);

        isInitialisingSwitch = true;
        switchDarkMode.setChecked(isDark);
        isInitialisingSwitch = false;

        tvDarkModeStatus.setText(isDark ? "On" : "Off");

        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isInitialisingSwitch) return;

            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupClickListeners() {
        if (rowEditProfile != null) {
            rowEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

        if (rowLogout != null) {
            rowLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        if (rowAboutApp != null) {
            rowAboutApp.setOnClickListener(v -> showAboutDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // Clear JWT token and user data
        ApiClient.logout();

        // Clear the local login flag
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_app_title)
                .setMessage(R.string.about_app_content)
                .setPositiveButton("OK", null)
                .show();
    }
}
