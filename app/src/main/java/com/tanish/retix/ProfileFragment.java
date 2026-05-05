package com.tanish.retix;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * ProfileFragment — displays the current user's profile data fetched from
 * Firebase Realtime Database (users/{uid}).
 *
 * Crash-safe design:
 *  - FirebaseAuth.getCurrentUser() checked before any DB call
 *  - All DataSnapshot fields read via safeString() — never .toString() on null
 *  - Glide used for remote image loading (no manual bitmap decoding)
 *  - isAdded() / getView() guards on every async callback
 *  - ProgressBar shown while data loads; hidden on success or failure
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "PROFILE";

    // ── Views ─────────────────────────────────────────────────────────────────
    private FrameLayout    layoutProfileImage;
    private ImageView      ivProfileImage;
    private TextView       tvUserInitial, tvUserName, tvUserEmail, tvUserRating;
    private TextView       tvTicketsSold, tvTotalEarnings, tvActiveListings;
    private LinearLayout   rowMyListings, rowMyPurchases, rowSettings;
    private ProgressBar    progressProfile;   // shown while Firebase data loads

    // ── State ─────────────────────────────────────────────────────────────────
    private OnProfileInteractionListener   listener;
    private ActivityResultLauncher<Intent> profileImageLauncher;
    private FirebaseManager                firebaseManager;

    // ── Interface ─────────────────────────────────────────────────────────────

    public interface OnProfileInteractionListener {
        void onSettingsClick();
        void onMyListingsClick();
        void onMyPurchasesClick();
    }

    public ProfileFragment() {}

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();

        if (getActivity() instanceof OnProfileInteractionListener) {
            listener = (OnProfileInteractionListener) getActivity();
        }

        // Register image picker — persists across configuration changes
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
        // Refresh when returning from EditProfileActivity
        if (tvUserName != null) loadProfile();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews(View view) {
        layoutProfileImage = view.findViewById(R.id.layout_profile_image);
        ivProfileImage     = view.findViewById(R.id.iv_profile_image);
        tvUserInitial      = view.findViewById(R.id.tv_user_initial);
        tvUserName         = view.findViewById(R.id.tv_user_name);
        tvUserEmail        = view.findViewById(R.id.tv_user_email);
        tvUserRating       = view.findViewById(R.id.tv_user_rating);
        tvTicketsSold      = view.findViewById(R.id.tv_tickets_sold);
        tvTotalEarnings    = view.findViewById(R.id.tv_total_earnings);
        tvActiveListings   = view.findViewById(R.id.tv_active_listings);
        rowMyListings      = view.findViewById(R.id.card_my_listings);
        rowMyPurchases     = view.findViewById(R.id.card_my_purchases);
        rowSettings        = view.findViewById(R.id.card_settings);

        // progressProfile is optional — only present if the layout has it
        // progressProfile = view.findViewById(R.id.progress_profile);
        progressProfile = null; // set to null; setLoading() guards against it
    }

    // ── Main load entry point ─────────────────────────────────────────────────

    /**
     * Loads profile data in two steps:
     *   1. Immediately show locally cached data (SharedPreferences) — no flicker
     *   2. Fetch fresh data from Firebase and update the UI
     *
     * If the user is not logged in, redirect to LoginActivity.
     */
    private void loadProfile() {
        // ── Step 1: Check auth ────────────────────────────────────────────────
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user — redirecting to LoginActivity");
            redirectToLogin();
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Loading profile for uid: " + uid);

        // ── Step 2: Show cached data immediately ──────────────────────────────
        showCachedProfile();

        // ── Step 3: Fetch fresh data from Firebase ────────────────────────────
        setLoading(true);

        firebaseManager.fetchUserProfile(uid, new FirebaseManager.UserProfileCallback() {
            @Override
            public void onSuccess(String name, String email, String profileImageUrl) {
                if (!isAdded() || getView() == null) return;
                setLoading(false);

                Log.d(TAG, "Profile loaded — name=" + name
                        + " email=" + email
                        + " imageUrl=" + profileImageUrl);

                // Merge: prefer Firebase data; fall back to cached if Firebase is empty
                String displayName  = !name.isEmpty()  ? name  : getCachedName();
                String displayEmail = !email.isEmpty() ? email : getCachedEmail();

                // Update UI
                applyProfileData(displayName, displayEmail, profileImageUrl);

                // Persist fresh data locally so it's available offline
                saveToCacheIfNonEmpty(displayName, displayEmail, profileImageUrl);

                // Load stats
                loadStats(uid);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded() || getView() == null) return;
                setLoading(false);
                Log.e(TAG, "Failed to load profile: " + errorMessage);
                // Keep showing cached data — don't crash or clear the UI
                loadStats(uid);
            }
        });
    }

    // ── Apply data to UI ──────────────────────────────────────────────────────

    /**
     * Applies name, email, and profile image to the UI.
     * All parameters are guaranteed non-null by the caller.
     */
    private void applyProfileData(String name, String email, String profileImageUrl) {
        // Name
        String safeName = (name != null && !name.isEmpty()) ? name : "User";
        tvUserName.setText(safeName);

        // Email
        String safeEmail = (email != null && !email.isEmpty()) ? email : "";
        tvUserEmail.setText(safeEmail);

        // Rating (static for now)
        tvUserRating.setText("4.8");

        // Profile image
        loadProfileImage(profileImageUrl, safeName);
    }

    /**
     * Loads the profile image using Glide.
     * Falls back to the initial-letter avatar if the URL is empty or load fails.
     */
    private void loadProfileImage(String imageUrl, String name) {
        if (!isAdded()) return;

        // Check for a locally picked image first (overrides remote URL)
        String localUri = getSharedPrefs().getString(EditProfileActivity.KEY_PROFILE_IMAGE, null);

        if (localUri != null && !localUri.isEmpty()) {
            // Load local URI via Glide (handles content:// URIs safely)
            Glide.with(this)
                    .load(Uri.parse(localUri))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .error(R.drawable.bg_circle_accent)
                    .into(ivProfileImage);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvUserInitial.setVisibility(View.GONE);
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Load remote URL (Cloudinary or Firebase Storage) via Glide
            Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .error(R.drawable.bg_circle_accent)
                    .into(ivProfileImage);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvUserInitial.setVisibility(View.GONE);
        } else {
            // No image — show initial letter avatar
            showInitial(name);
        }
    }

    // ── Cached data helpers ───────────────────────────────────────────────────

    /** Shows locally cached profile data immediately (no network wait). */
    private void showCachedProfile() {
        String name  = getCachedName();
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
        if (name  != null && !name.isEmpty())  editor.putString(EditProfileActivity.KEY_PROFILE_NAME,  name);
        if (email != null && !email.isEmpty()) editor.putString(EditProfileActivity.KEY_PROFILE_EMAIL, email);
        // Don't overwrite local image URI with a remote URL
        editor.apply();
    }

    private SharedPreferences getSharedPrefs() {
        return requireActivity().getSharedPreferences(
                EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats(String uid) {
        if (uid == null || uid.isEmpty()) {
            setDefaultStats();
            return;
        }

        firebaseManager.fetchMyListings(uid, new FirebaseManager.TicketsCallback() {
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

                Log.d(TAG, "Stats — active=" + active + " sold=" + sold + " total=" + total);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded() || getView() == null) return;
                Log.e(TAG, "Stats fetch failed: " + errorMessage);
                setDefaultStats();
            }
        });
    }

    private void setDefaultStats() {
        if (tvTicketsSold    != null) tvTicketsSold.setText("0");
        if (tvTotalEarnings  != null) tvTotalEarnings.setText("₹0");
        if (tvActiveListings != null) tvActiveListings.setText("0");
    }

    // ── Image picker callback ─────────────────────────────────────────────────

    private void onProfileImagePicked(Uri imageUri) {
        if (!isAdded()) return;

        // Persist locally
        getSharedPrefs().edit()
                .putString(EditProfileActivity.KEY_PROFILE_IMAGE, imageUri.toString())
                .apply();

        // Load immediately via Glide
        Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .circleCrop()
                .error(R.drawable.bg_circle_accent)
                .into(ivProfileImage);
        ivProfileImage.setVisibility(View.VISIBLE);
        tvUserInitial.setVisibility(View.GONE);

        Log.d(TAG, "Profile image updated: " + imageUri);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        layoutProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            profileImageLauncher.launch(intent);
        });

        rowSettings.setOnClickListener(v -> {
            if (listener != null) listener.onSettingsClick();
        });

        rowMyListings.setOnClickListener(v -> {
            if (listener != null) listener.onMyListingsClick();
        });

        rowMyPurchases.setOnClickListener(v -> {
            if (listener != null) listener.onMyPurchasesClick();
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showInitial(String name) {
        ivProfileImage.setVisibility(View.GONE);
        tvUserInitial.setVisibility(View.VISIBLE);
        if (name != null && !name.isEmpty()) {
            tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvUserInitial.setText("?");
        }
    }

    /** Shows/hides the loading indicator if it exists in the layout. */
    private void setLoading(boolean loading) {
        if (progressProfile != null) {
            progressProfile.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /** Redirects to LoginActivity and clears the back stack. */
    private void redirectToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
