package com.tanish.retix;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";

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
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imageLauncher;
    private ApiManager apiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        apiManager = ApiManager.getInstance();

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

        // Find or create progress bar
        progressBar = findViewById(R.id.progress_bar);
        if (progressBar == null) {
            // Progress bar might be in layout, if not we'll handle visibility differently
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }
    }

    // ── Load saved data ───────────────────────────────────────────────────────

    private void loadSavedProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String name  = prefs.getString(KEY_PROFILE_NAME,  "");
        String email = prefs.getString(KEY_PROFILE_EMAIL, "");
        String imageUriStr = prefs.getString(KEY_PROFILE_IMAGE, null);

        etName.setText(name);
        etEmail.setText(email);

        // Show saved profile image or initial letter
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            if (imageUriStr.startsWith("http")) {
                // Server URL - use Glide-compatible display
                // Don't set selectedImageUri since this is already uploaded
                showProfileImageFromUrl(imageUriStr);
            } else {
                // Local URI - might be from old version or not yet uploaded
                try {
                    Uri uri = Uri.parse(imageUriStr);
                    selectedImageUri = uri;
                    showProfileImage(uri);
                } catch (Exception e) {
                    showInitial(name);
                }
            }
        } else {
            showInitial(name);
        }

        // Also fetch from server to get latest data
        fetchProfileFromServer();
    }

    private void fetchProfileFromServer() {
        String userId = ApiClient.getTokenManager().getUserId();
        if (userId == null) return;

        apiManager.fetchUserProfile(userId, new ApiManager.UserProfileCallback() {
            @Override
            public void onSuccess(String name, String email, String profileImageUrl) {
                // Update UI with server data if fields are empty
                if (etName.getText().toString().trim().isEmpty() && !name.isEmpty()) {
                    etName.setText(name);
                }
                if (etEmail.getText().toString().trim().isEmpty() && !email.isEmpty()) {
                    etEmail.setText(email);
                }
                // Update image if server has one and no local image is selected
                if (selectedImageUri == null && profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    showProfileImageFromUrl(profileImageUrl);
                    // Save server URL
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putString(KEY_PROFILE_IMAGE, profileImageUrl)
                            .apply();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.w(TAG, "Failed to fetch profile: " + errorMessage);
            }
        });
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

        setLoading(true);

        // If there's a new image, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(name, email);
        } else {
            // No new image, just update profile
            updateProfile(name, email, null);
        }
    }

    private void uploadImageAndSaveProfile(String name, String email) {
        try {
            MultipartBody.Part imagePart = createImagePart(selectedImageUri);
            if (imagePart == null) {
                setLoading(false);
                Toast.makeText(this, "Failed to prepare image", Toast.LENGTH_SHORT).show();
                return;
            }

            apiManager.uploadImage(imagePart, new ApiManager.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Image uploaded, now update profile with the URL
                    updateProfile(name, email, imageUrl);
                }

                @Override
                public void onFailure(String errorMessage) {
                    setLoading(false);
                    Log.e(TAG, "Image upload failed: " + errorMessage);
                    Toast.makeText(EditProfileActivity.this,
                            "Image upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            setLoading(false);
            Log.e(TAG, "Error preparing image: " + e.getMessage());
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private MultipartBody.Part createImagePart(Uri imageUri) throws IOException {
        // Create a temporary file from the URI
        File tempFile = createTempFileFromUri(imageUri);
        if (tempFile == null) return null;

        // Create RequestBody from the file
        RequestBody requestFile = RequestBody.create(
                MediaType.parse(getContentResolver().getType(imageUri) != null ?
                        getContentResolver().getType(imageUri) : "image/jpeg"),
                tempFile
        );

        // Create MultipartBody.Part
        return MultipartBody.Part.createFormData("image", tempFile.getName(), requestFile);
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        File tempFile = new File(getCacheDir(), "upload_image_" + System.currentTimeMillis() + ".jpg");

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) return null;

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    private void updateProfile(String name, String email, String imageUrl) {
        // Update user profile via API
        apiManager.saveUser(ApiClient.getTokenManager().getUserId(), name, email, imageUrl,
                new ApiManager.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        // Save to SharedPreferences
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(KEY_PROFILE_NAME, name);
                        editor.putString(KEY_PROFILE_EMAIL, email);
                        // Save SERVER URL (not local URI) - imageUrl can be null if no new image
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            editor.putString(KEY_PROFILE_IMAGE, imageUrl);
                        }
                        editor.apply();

                        // Update TokenManager cache
                        ApiClient.getTokenManager().updateUserName(name);

                        setLoading(false);
                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // Return to Settings with a success result so ProfileFragment can refresh
                        setResult(Activity.RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Log.e(TAG, "Profile update failed: " + errorMessage);
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!loading);
        layoutAvatar.setEnabled(!loading);
        btnBack.setEnabled(!loading);
    }

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

    private void showProfileImageFromUrl(String url) {
        try {
            Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.bg_circle_accent)
                    .error(R.drawable.bg_circle_accent)
                    .into(ivProfileImage);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvUserInitial.setVisibility(View.GONE);
        } catch (Exception e) {
            // URL failed — fall back to initial
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

    // ── Simple TextWatcher helper ─────────────────────────────────────────────

    private abstract static class SimpleTextWatcher
            implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
        public abstract void onChanged();
    }
}
