package com.tanish.retix;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class ProfileFragment extends Fragment {

    private FrameLayout layoutProfileImage;
    private ImageView ivProfileImage;
    private TextView tvUserInitial, tvUserName, tvUserEmail, tvUserRating;
    private TextView tvTicketsSold, tvTotalEarnings, tvActiveListings;
    // Menu rows are now LinearLayouts
    private LinearLayout rowMyListings, rowMyPurchases, rowSettings;

    private OnProfileInteractionListener listener;
    private ActivityResultLauncher<Intent> profileImageLauncher;

    public interface OnProfileInteractionListener {
        void onSettingsClick();
        void onMyListingsClick();
        void onMyPurchasesClick();
    }

    public ProfileFragment() {}

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof OnProfileInteractionListener) {
            listener = (OnProfileInteractionListener) getActivity();
        }

        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && ivProfileImage != null) {
                            ivProfileImage.setImageURI(imageUri);
                            ivProfileImage.setVisibility(View.VISIBLE);
                            tvUserInitial.setVisibility(View.GONE);
                            // Persist so EditProfile and future resumes see the same image
                            requireActivity()
                                    .getSharedPreferences(EditProfileActivity.PREFS_NAME,
                                            android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putString(EditProfileActivity.KEY_PROFILE_IMAGE,
                                            imageUri.toString())
                                    .apply();
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
        setupUserData();
        setupStats();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data in case user just returned from EditProfileActivity
        if (tvUserName != null) setupUserData();
    }

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
    }

    private void setupUserData() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(EditProfileActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);

        String userName  = prefs.getString(EditProfileActivity.KEY_PROFILE_NAME,  "John Doe");
        String userEmail = prefs.getString(EditProfileActivity.KEY_PROFILE_EMAIL, "john.doe@example.com");
        String imageUriStr = prefs.getString(EditProfileActivity.KEY_PROFILE_IMAGE, null);
        float  rating    = 4.8f;

        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        tvUserRating.setText(String.format("%.1f", rating));

        // Show saved profile image or initial letter
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            try {
                android.net.Uri uri = android.net.Uri.parse(imageUriStr);
                ivProfileImage.setImageURI(uri);
                ivProfileImage.setVisibility(View.VISIBLE);
                tvUserInitial.setVisibility(View.GONE);
            } catch (Exception e) {
                showInitial(userName);
            }
        } else {
            showInitial(userName);
        }
    }

    private void showInitial(String name) {
        ivProfileImage.setVisibility(View.GONE);
        tvUserInitial.setVisibility(View.VISIBLE);
        if (name != null && !name.isEmpty()) {
            tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvUserInitial.setText("?");
        }
    }

    private void setupStats() {
        // Dummy stats — replace with real data when backend is ready
        tvTicketsSold.setText("12");
        tvTotalEarnings.setText("₹36k");
        tvActiveListings.setText("3");
    }

    private void setupClickListeners() {
        // Tap profile image area to pick photo
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
}
