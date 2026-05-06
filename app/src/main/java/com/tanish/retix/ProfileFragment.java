package com.tanish.retix;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * ProfileFragment — Displays user profile with skeleton loading,
 * edit profile, logout, and user stats.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "PROFILE";

    // Views
    private FrameLayout layoutProfileImage;
    private ImageView ivProfileImage;
    private TextView tvUserInitial, tvUserName, tvUserEmail;
    private TextView tvTicketsSold, tvTotalEarnings, tvActiveListings;
    private ProgressBar progressProfile;
    private View skeletonContainer;
    private View contentContainer;
    private View headerSkeleton;
    private View headerContent;
    private MaterialButton btnEditProfile;
    private MaterialButton btnLogout;
    private ImageButton btnSettings;

    // State
    private ActivityResultLauncher<Intent> profileImageLauncher;
    private ApiManager apiManager;
    private boolean isLoading = false;
    private boolean hasProfileData = false;

    public ProfileFragment() {}

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiManager = ApiManager.getInstance();

        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            onProfileImagePicked(imageUri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupClickListeners();
        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always refresh profile on resume to pick up changes from EditProfileActivity
        loadProfile();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (skeletonContainer != null) {
            skeletonContainer.setVisibility(View.GONE);
        }
        if (headerSkeleton != null) {
            headerSkeleton.setVisibility(View.GONE);
        }
    }

    private void initViews(View view) {
        layoutProfileImage = view.findViewById(R.id.layout_profile_image);
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        tvUserInitial = view.findViewById(R.id.tv_user_initial);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);

        // Stat cards use included item_stat_card layout
        MaterialCardView cardSold = view.findViewById(R.id.card_tickets_sold);
        MaterialCardView cardEarnings = view.findViewById(R.id.card_earnings);
        MaterialCardView cardActive = view.findViewById(R.id.card_active_listings);

        cardSold.setCardBackgroundColor(getResources().getColor(R.color.primary_dark));
        cardEarnings.setCardBackgroundColor(getResources().getColor(R.color.accent_blue));
        cardActive.setCardBackgroundColor(getResources().getColor(R.color.success_green));

        tvTicketsSold = cardSold.findViewById(R.id.tv_stat_value);
        tvTotalEarnings = cardEarnings.findViewById(R.id.tv_stat_value);
        tvActiveListings = cardActive.findViewById(R.id.tv_stat_value);

        // Set static icon and label text for each card
        ((ImageView) cardSold.findViewById(R.id.iv_stat_icon)).setImageResource(R.drawable.ic_ticket);
        ((TextView) cardSold.findViewById(R.id.tv_stat_label)).setText("Sold");
        ((ImageView) cardEarnings.findViewById(R.id.iv_stat_icon)).setImageResource(R.drawable.ic_money);
        ((TextView) cardEarnings.findViewById(R.id.tv_stat_label)).setText("Earned");
        ((ImageView) cardActive.findViewById(R.id.iv_stat_icon)).setImageResource(R.drawable.ic_list);
        ((TextView) cardActive.findViewById(R.id.tv_stat_label)).setText("Active");

        progressProfile = view.findViewById(R.id.progress_profile);
        skeletonContainer = view.findViewById(R.id.skeleton_container);
        contentContainer = view.findViewById(R.id.content_container);
        headerSkeleton = view.findViewById(R.id.header_skeleton);
        headerContent = view.findViewById(R.id.header_content);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnSettings = view.findViewById(R.id.btn_settings);
    }

    private void loadProfile() {
        String userId = ApiClient.getTokenManager().getUserId();
        if (userId == null) {
            Log.w(TAG, "No authenticated user — redirecting to LoginActivity");
            redirectToLogin();
            return;
        }

        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate request");
            return;
        }

        Log.d(TAG, "Loading profile for uid: " + userId);
        isLoading = true;
        showSkeleton();

        apiManager.fetchUserProfile(userId, new ApiManager.UserProfileCallback() {
            @Override
            public void onSuccess(String name, String email, String profileImageUrl) {
                isLoading = false;
                if (!isAdded() || getView() == null) return;

                String displayName = !name.isEmpty() ? name : getCachedName();
                String displayEmail = !email.isEmpty() ? email : getCachedEmail();

                applyProfileData(displayName, displayEmail, profileImageUrl);
                saveToCacheIfNonEmpty(displayName, displayEmail, profileImageUrl);
                loadStats(userId);
            }

            @Override
            public void onFailure(String errorMessage) {
                isLoading = false;
                if (!isAdded() || getView() == null) return;
                Log.e(TAG, "Failed to load profile: " + errorMessage);
                showCachedProfile();
                loadStats(userId);
            }
        });
    }

    private void applyProfileData(String name, String email, String profileImageUrl) {
        String safeName = (name != null && !name.isEmpty()) ? name : "User";
        tvUserName.setText(safeName);

        String safeEmail = (email != null && !email.isEmpty()) ? email : "";
        tvUserEmail.setText(safeEmail);

        loadProfileImage(profileImageUrl, safeName);
        hasProfileData = true;
        showContent();
    }

    private void loadProfileImage(String imageUrl, String name) {
        if (!isAdded()) return;

        // Prioritize server URL - it's more reliable than local URI
        String serverImageUrl = imageUrl;
        if (serverImageUrl == null || serverImageUrl.isEmpty()) {
            serverImageUrl = getSharedPrefs().getString(EditProfileActivity.KEY_PROFILE_IMAGE, null);
        }

        // Check if the saved value is a URL (http/https) or local URI
        if (serverImageUrl != null && !serverImageUrl.isEmpty()) {
            if (serverImageUrl.startsWith("http")) {
                // Server URL - use Glide with caching
                Glide.with(this)
                        .load(serverImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .placeholder(R.drawable.bg_circle_accent)
                        .error(R.drawable.bg_circle_accent)
                        .into(ivProfileImage);
                ivProfileImage.setVisibility(View.VISIBLE);
                tvUserInitial.setVisibility(View.GONE);
                return;
            } else {
                // Local URI - try to load but may fail if permission expired
                try {
                    Glide.with(this)
                            .load(Uri.parse(serverImageUrl))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .circleCrop()
                            .placeholder(R.drawable.bg_circle_accent)
                            .error(R.drawable.bg_circle_accent)
                            .into(ivProfileImage);
                    ivProfileImage.setVisibility(View.VISIBLE);
                    tvUserInitial.setVisibility(View.GONE);
                    return;
                } catch (Exception e) {
                    // Local URI failed, fall through to initial
                    Log.w(TAG, "Failed to load local URI: " + e.getMessage());
                }
            }
        }

        showInitial(name);
    }

    private void showCachedProfile() {
        String name = getCachedName();
        String email = getCachedEmail();
        applyProfileData(name, email, "");
    }

    private String getCachedName() {
        return getSharedPrefs().getString(EditProfileActivity.KEY_PROFILE_NAME, "");
    }

    private String getCachedEmail() {
        return getSharedPrefs().getString(EditProfileActivity.KEY_PROFILE_EMAIL, "");
    }

    private void saveToCacheIfNonEmpty(String name, String email, String imageUrl) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        if (name != null && !name.isEmpty()) editor.putString(EditProfileActivity.KEY_PROFILE_NAME, name);
        if (email != null && !email.isEmpty()) editor.putString(EditProfileActivity.KEY_PROFILE_EMAIL, email);
        // Only save server image URL if no local image is set
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String localUri = getSharedPrefs().getString(EditProfileActivity.KEY_PROFILE_IMAGE, null);
            if (localUri == null || localUri.isEmpty()) {
                editor.putString(EditProfileActivity.KEY_PROFILE_IMAGE, imageUrl);
            }
        }
        editor.apply();

        // Also update TokenManager
        ApiClient.getTokenManager().updateUserInfo(name, email);
    }

    private SharedPreferences getSharedPrefs() {
        return requireActivity().getSharedPreferences(
                EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Stats
    private void loadStats(String uid) {
        apiManager.fetchMyListings(uid, new ApiManager.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                if (!isAdded() || getView() == null) return;

                int active = 0, sold = 0, total = 0;
                for (Ticket t : tickets) {
                    if (t == null) continue;
                    if (Ticket.STATUS_AVAILABLE.equals(t.getStatus())) {
                        active++;
                    } else {
                        sold++;
                        total += t.getSellingPrice();
                    }
                }

                tvActiveListings.setText(String.valueOf(active));
                tvTicketsSold.setText(String.valueOf(sold));
                tvTotalEarnings.setText(total >= 1000
                        ? "₹" + (total / 1000) + "k"
                        : "₹" + total);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded() || getView() == null) return;
                setDefaultStats();
            }
        });
    }

    private void setDefaultStats() {
        if (tvTicketsSold != null) tvTicketsSold.setText("0");
        if (tvTotalEarnings != null) tvTotalEarnings.setText("₹0");
        if (tvActiveListings != null) tvActiveListings.setText("0");
    }

    // Image picker
    private void onProfileImagePicked(Uri imageUri) {
        if (!isAdded()) return;

        // Show image immediately
        Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .circleCrop()
                .error(R.drawable.bg_circle_accent)
                .into(ivProfileImage);
        ivProfileImage.setVisibility(View.VISIBLE);
        tvUserInitial.setVisibility(View.GONE);

        // Upload image to server
        uploadProfileImage(imageUri);
    }

    private void uploadProfileImage(Uri imageUri) {
        showLoading(true);

        new Thread(() -> {
            try {
                // Create multipart from URI
                java.io.File tempFile = createTempFileFromUri(imageUri);
                if (tempFile == null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse(requireContext().getContentResolver().getType(imageUri) != null ?
                                requireContext().getContentResolver().getType(imageUri) : "image/jpeg"),
                        tempFile
                );
                okhttp3.MultipartBody.Part imagePart = okhttp3.MultipartBody.Part.createFormData("image", tempFile.getName(), requestFile);

                requireActivity().runOnUiThread(() -> {
                    apiManager.uploadImage(imagePart, new ApiManager.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            // Save SERVER URL (not local URI) for display
                            getSharedPrefs().edit()
                                    .putString(EditProfileActivity.KEY_PROFILE_IMAGE, imageUrl)
                                    .apply();

                            // Update profile with new image URL
                            updateProfileImageUrl(imageUrl);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private java.io.File createTempFileFromUri(android.net.Uri uri) throws java.io.IOException {
        java.io.File tempFile = new java.io.File(requireContext().getCacheDir(), "profile_image_" + System.currentTimeMillis() + ".jpg");

        try (java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {

            if (inputStream == null) return null;

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    private void updateProfileImageUrl(String imageUrl) {
        String name = tvUserName.getText().toString().trim();
        String email = tvUserEmail.getText().toString().trim();

        apiManager.saveUser(ApiClient.getTokenManager().getUserId(), name, email, imageUrl,
                new ApiManager.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                        // Refresh profile from server
                        loadProfile();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressProfile != null) {
            progressProfile.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // Click listeners
    private void setupClickListeners() {
        layoutProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            profileImageLauncher.launch(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        ApiClient.logout();
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReTixPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_logged_in", false).apply();
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // UI helpers
    private void showInitial(String name) {
        ivProfileImage.setVisibility(View.GONE);
        tvUserInitial.setVisibility(View.VISIBLE);
        if (name != null && !name.isEmpty()) {
            tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvUserInitial.setText("?");
        }
    }

    private void showSkeleton() {
        if (headerSkeleton != null) headerSkeleton.setVisibility(View.VISIBLE);
        if (headerContent != null) headerContent.setVisibility(View.GONE);
        if (skeletonContainer != null) skeletonContainer.setVisibility(View.VISIBLE);
        if (contentContainer != null) contentContainer.setVisibility(View.GONE);
        if (progressProfile != null) progressProfile.setVisibility(View.GONE);
    }

    private void showContent() {
        if (headerSkeleton != null) headerSkeleton.setVisibility(View.GONE);
        if (headerContent != null) headerContent.setVisibility(View.VISIBLE);
        if (skeletonContainer != null) skeletonContainer.setVisibility(View.GONE);
        if (contentContainer != null) contentContainer.setVisibility(View.VISIBLE);
    }

    private void redirectToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}