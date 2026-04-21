package com.tanish.retix;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    // SharedPreferences keys — shared with ProfileFragment
    static final String PREFS_NAME        = "ReTixPrefs";
    static final String KEY_PROFILE_NAME  = "profile_name";
    static final String KEY_PROFILE_EMAIL = "profile_email";
    static final String KEY_PROFILE_IMAGE = "profile_image_uri";

    private ImageButton btnBack;
    private FrameLayout layoutAvatar;
    private ImageView ivProfileImage;
    private TextView tvUserInitial;
    private EditText etName, etEmail;
    private TextView tvErrorName, tvErrorEmail;
    private MaterialButton btnSave;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        registerImageLauncher();
        initViews();
        loadSavedProfile();
        setupListeners();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    private void registerImageLauncher() {
        imageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            showProfileImage(uri);
                        }
                    }
                });
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews() {
        btnBack        = findViewById(R.id.btn_back);
        layoutAvatar   = findViewById(R.id.layout_avatar);
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvUserInitial  = findViewById(R.id.tv_user_initial);
        etName         = findViewById(R.id.et_name);
        etEmail        = findViewById(R.id.et_email);
        tvErrorName    = findViewById(R.id.tv_error_name);
        tvErrorEmail   = findViewById(R.id.tv_error_email);
        btnSave        = findViewById(R.id.btn_save);
    }

    // ── Load saved data ───────────────────────────────────────────────────────

    private void loadSavedProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String name  = prefs.getString(KEY_PROFILE_NAME,  "John Doe");
        String email = prefs.getString(KEY_PROFILE_EMAIL, "john.doe@example.com");
        String imageUriStr = prefs.getString(KEY_PROFILE_IMAGE, null);

        etName.setText(name);
        etEmail.setText(email);

        // Show saved profile image or initial letter
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            try {
                Uri uri = Uri.parse(imageUriStr);
                selectedImageUri = uri;
                showProfileImage(uri);
            } catch (Exception e) {
                showInitial(name);
            }
        } else {
            showInitial(name);
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutAvatar.setOnClickListener(v -> pickImage());

        // Clear errors as user types
        etName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onChanged() { tvErrorName.setVisibility(View.GONE); }
        });
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onChanged() { tvErrorEmail.setVisibility(View.GONE); }
        });

        btnSave.setOnClickListener(v -> attemptSave());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imageLauncher.launch(intent);
    }

    // ── Save logic ────────────────────────────────────────────────────────────

    private void attemptSave() {
        String name  = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        tvErrorName.setVisibility(View.GONE);
        tvErrorEmail.setVisibility(View.GONE);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            tvErrorName.setText("Name cannot be empty");
            tvErrorName.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tvErrorEmail.setText("Email cannot be empty");
            tvErrorEmail.setVisibility(View.VISIBLE);
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvErrorEmail.setText("Please enter a valid email address");
            tvErrorEmail.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (!valid) return;

        // Persist to SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_PROFILE_NAME, name);
        editor.putString(KEY_PROFILE_EMAIL, email);
        if (selectedImageUri != null) {
            editor.putString(KEY_PROFILE_IMAGE, selectedImageUri.toString());
        }
        editor.apply();

        // Update the initial letter in case name changed
        showInitialForName(name);

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

        // Return to Settings with a success result so ProfileFragment can refresh
        setResult(Activity.RESULT_OK);
        finish();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showProfileImage(Uri uri) {
        try {
            ivProfileImage.setImageURI(uri);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvUserInitial.setVisibility(View.GONE);
        } catch (Exception e) {
            // URI not accessible — fall back to initial
            showInitial(etName.getText().toString().trim());
        }
    }

    private void showInitial(String name) {
        ivProfileImage.setVisibility(View.GONE);
        tvUserInitial.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(name)) {
            tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvUserInitial.setText("?");
        }
    }

    private void showInitialForName(String name) {
        if (ivProfileImage.getVisibility() != View.VISIBLE) {
            showInitial(name);
        }
    }

    // ── Simple TextWatcher helper ─────────────────────────────────────────────

    private abstract static class SimpleTextWatcher
            implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
        public abstract void onChanged();
    }
}
