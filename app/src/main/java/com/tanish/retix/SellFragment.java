package com.tanish.retix;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SellFragment extends Fragment {

    private static final String TAG = "SELL";

    // Safety timeout: if neither success nor failure fires within this many ms,
    // dismiss the loading dialog and re-enable the button automatically.
    private static final long UPLOAD_TIMEOUT_MS = 120_000; // 2 minutes

    // ── Form fields ───────────────────────────────────────────────────────────
    private EditText       etEventName, etDate, etOriginalPrice, etSellingPrice;
    private TextView       tvRecoveryMessage;
    private MaterialButton btnSubmit;
    private View           rootView;

    // ── Event image upload ────────────────────────────────────────────────────
    private LinearLayout   layoutEventImagePicker;
    private FrameLayout    frameEventImage;
    private ImageView      ivEventImagePreview;
    private MaterialButton btnChangeEventImage, btnRemoveEventImage;

    // ── Ticket file upload ────────────────────────────────────────────────────
    private LinearLayout   layoutTicketFilePicker;
    private LinearLayout   frameTicketFile;
    private TextView       tvTicketFileName;
    private TextView       tvTicketUploadError;
    private MaterialButton btnChangeTicketFile, btnRemoveTicketFile;

    // ── State ─────────────────────────────────────────────────────────────────
    private Uri selectedEventImageUri = null;
    private Uri selectedTicketFileUri = null;

    private Calendar         selectedDate;
    private SimpleDateFormat dateFormat;
    private LoadingDialog    loadingDialog;
    private FirebaseManager  firebaseManager;

    // Safety timeout handler — cancels infinite loading
    private final Handler  timeoutHandler  = new Handler(Looper.getMainLooper());
    private       Runnable timeoutRunnable = null;

    // ── Activity result launchers ─────────────────────────────────────────────
    private ActivityResultLauncher<Intent> eventImageLauncher;
    private ActivityResultLauncher<Intent> ticketFileLauncher;

    public SellFragment() {}

    public static SellFragment newInstance() {
        return new SellFragment();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();

        eventImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) onEventImageSelected(uri);
                    }
                });

        ticketFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) onTicketFileSelected(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView      = view;
        loadingDialog = new LoadingDialog(requireContext());
        initViews(view);
        setupDatePicker();
        setupPricingListener();
        setupUploadButtons();
        setupSubmitButton();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTimeout();
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews(View view) {
        etEventName       = view.findViewById(R.id.et_event_name);
        etDate            = view.findViewById(R.id.et_date);
        etOriginalPrice   = view.findViewById(R.id.et_original_price);
        etSellingPrice    = view.findViewById(R.id.et_selling_price);
        tvRecoveryMessage = view.findViewById(R.id.tv_recovery_message);
        btnSubmit         = view.findViewById(R.id.btn_submit);

        layoutEventImagePicker = view.findViewById(R.id.layout_event_image_picker);
        frameEventImage        = view.findViewById(R.id.frame_event_image);
        ivEventImagePreview    = view.findViewById(R.id.iv_event_image_preview);
        btnChangeEventImage    = view.findViewById(R.id.btn_change_event_image);
        btnRemoveEventImage    = view.findViewById(R.id.btn_remove_event_image);

        layoutTicketFilePicker = view.findViewById(R.id.layout_ticket_file_picker);
        frameTicketFile        = view.findViewById(R.id.frame_ticket_file);
        tvTicketFileName       = view.findViewById(R.id.tv_ticket_file_name);
        tvTicketUploadError    = view.findViewById(R.id.tv_ticket_upload_error);
        btnChangeTicketFile    = view.findViewById(R.id.btn_change_ticket_file);
        btnRemoveTicketFile    = view.findViewById(R.id.btn_remove_ticket_file);

        selectedDate = Calendar.getInstance();
        dateFormat   = new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault());
    }

    // ── Date picker ───────────────────────────────────────────────────────────

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (dp, year, month, day) -> {
                        selectedDate.set(year, month, day);
                        etDate.setText(dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    // ── Pricing listener ──────────────────────────────────────────────────────

    private void setupPricingListener() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateRecoveryMessage(); }
        };
        etSellingPrice.addTextChangedListener(watcher);
        etOriginalPrice.addTextChangedListener(watcher);
    }

    private void updateRecoveryMessage() {
        String str = etSellingPrice.getText().toString().trim();
        if (str.isEmpty()) {
            tvRecoveryMessage.setText(getString(R.string.enter_selling_price));
            tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.text_secondary));
            return;
        }
        try {
            int price = Integer.parseInt(str);
            if (price <= 0) {
                tvRecoveryMessage.setText("Please enter a valid price");
                tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.error_red));
            } else {
                tvRecoveryMessage.setText("🟢 Recover ₹" + price + " from your ticket");
                tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.success_green));
            }
        } catch (NumberFormatException e) {
            tvRecoveryMessage.setText("Please enter a valid price");
            tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.error_red));
        }
    }

    // ── Upload buttons ────────────────────────────────────────────────────────

    private void setupUploadButtons() {
        layoutEventImagePicker.setOnClickListener(v -> pickEventImage());
        btnChangeEventImage.setOnClickListener(v -> pickEventImage());
        btnRemoveEventImage.setOnClickListener(v -> {
            selectedEventImageUri = null;
            ivEventImagePreview.setImageDrawable(null);
            frameEventImage.setVisibility(View.GONE);
            layoutEventImagePicker.setVisibility(View.VISIBLE);
        });

        layoutTicketFilePicker.setOnClickListener(v -> pickTicketFile());
        btnChangeTicketFile.setOnClickListener(v -> pickTicketFile());
        btnRemoveTicketFile.setOnClickListener(v -> {
            selectedTicketFileUri = null;
            frameTicketFile.setVisibility(View.GONE);
            layoutTicketFilePicker.setVisibility(View.VISIBLE);
            layoutTicketFilePicker.setBackground(
                    requireContext().getDrawable(R.drawable.bg_upload_area));
            tvTicketUploadError.setVisibility(View.GONE);
        });
    }

    private void pickEventImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        eventImageLauncher.launch(intent);
    }

    private void pickTicketFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        ticketFileLauncher.launch(intent);
    }

    private void onEventImageSelected(Uri uri) {
        selectedEventImageUri = uri;
        ivEventImagePreview.setImageURI(uri);
        frameEventImage.setVisibility(View.VISIBLE);
        layoutEventImagePicker.setVisibility(View.GONE);
    }

    private void onTicketFileSelected(Uri uri) {
        selectedTicketFileUri = uri;
        tvTicketFileName.setText(getFileName(uri));
        frameTicketFile.setVisibility(View.VISIBLE);
        layoutTicketFilePicker.setVisibility(View.GONE);
        tvTicketUploadError.setVisibility(View.GONE);
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) submitTicket();
        });
    }

    /**
     * Submit flow — strictly ordered:
     *   1. Validate form
     *   2. Start safety timeout (prevents infinite loading)
     *   3. If image selected → upload to Cloudinary → wait for secure_url
     *   4. ONLY after URL received → write ticket to Firebase
     *   5. On success OR failure → ALWAYS stop loading + re-enable button
     */
    private void submitTicket() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "You must be logged in to list a ticket",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Snapshot all form values NOW before any async work begins.
        // This prevents clearForm() from wiping them before callbacks fire.
        final String sellerId   = user.getUid();
        final String title      = etEventName.getText().toString().trim();
        final String eventDate  = etDate.getText().toString().trim();
        final int    price      = parsePrice(etSellingPrice);
        final String savedName  = requireActivity()
                .getSharedPreferences(EditProfileActivity.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE)
                .getString(EditProfileActivity.KEY_PROFILE_NAME, null);
        final String sellerName = (savedName != null && !savedName.isEmpty())
                ? savedName
                : (user.getDisplayName() != null ? user.getDisplayName() : "Seller");

        // Lock UI
        setSubmitLoading(true);
        loadingDialog.setMessage("Preparing...");
        loadingDialog.show();

        // Start safety timeout — if nothing responds in UPLOAD_TIMEOUT_MS, stop loading
        startTimeout("Upload timed out. Please check your connection and try again.");

        if (selectedEventImageUri != null) {
            // ── STEP 1: Upload image to Cloudinary ────────────────────────────
            loadingDialog.setMessage("Uploading image...");
            Log.d(TAG, "submitTicket: uploading image uri=" + selectedEventImageUri);

            CloudinaryClient.uploadImage(
                    requireContext(),
                    selectedEventImageUri,
                    new CloudinaryClient.UploadCallback() {

                        @Override
                        public void onSuccess(String secureUrl) {
                            // Callback is guaranteed on main thread by CloudinaryClient
                            if (!isAdded()) {
                                Log.w(TAG, "onSuccess: fragment detached, aborting");
                                return;
                            }
                            cancelTimeout(); // upload succeeded — cancel the safety timer
                            Log.d(TAG, "uploadImage: success — secure_url=" + secureUrl);

                            // ── STEP 2: Save to Firebase with the URL ─────────
                            saveTicketToFirebase(sellerId, sellerName, title,
                                    eventDate, price, secureUrl);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (!isAdded()) return;
                            cancelTimeout();
                            Log.e(TAG, "uploadImage: failed — " + errorMessage);
                            // ALWAYS stop loading on failure
                            stopLoading();
                            showError("Image upload failed: " + errorMessage);
                        }
                    });
        } else {
            // No image — go straight to Firebase
            cancelTimeout(); // no upload step, cancel immediately
            Log.d(TAG, "submitTicket: no image selected, skipping Cloudinary");
            saveTicketToFirebase(sellerId, sellerName, title, eventDate, price, "");
        }
    }

    // ── Firebase write ────────────────────────────────────────────────────────

    /**
     * Writes the ticket to Firebase Realtime Database.
     * Called ONLY after the Cloudinary URL has been received (or skipped).
     * ALWAYS stops loading in both success and failure paths.
     */
    private void saveTicketToFirebase(String sellerId, String sellerName,
                                      String title, String eventDate,
                                      int price, String imageUrl) {
        if (!isAdded()) return;

        loadingDialog.setMessage("Saving ticket...");

        // Start a fresh timeout for the Firebase write
        startTimeout("Ticket save timed out. Please try again.");

        Log.d(TAG, "saveTicketToFirebase: title=" + title
                + " price=" + price
                + " imageUrl=" + (imageUrl.isEmpty() ? "(none)" : imageUrl)
                + " sellerId=" + sellerId);

        firebaseManager.saveTicket(
                title,
                title,       // description = title
                price,
                eventDate,
                imageUrl,
                sellerId,
                sellerName,
                new FirebaseManager.VoidCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        cancelTimeout();
                        Log.d(TAG, "saveTicketToFirebase: success");

                        // ALWAYS stop loading first
                        stopLoading();

                        Toast.makeText(requireContext(),
                                "Ticket listed successfully!", Toast.LENGTH_SHORT).show();

                        // Credit wallet — fire-and-forget, doesn't block the UI
                        if (price > 0) {
                            firebaseManager.addMoney(
                                    sellerId,
                                    price,
                                    "Ticket listed: " + title,
                                    new FirebaseManager.VoidCallback() {
                                        @Override public void onSuccess() {
                                            Log.d("WALLET", "Credited ₹" + price
                                                    + " for: " + title);
                                        }
                                        @Override public void onFailure(String err) {
                                            Log.e("WALLET", "Credit failed: " + err);
                                        }
                                    });
                        }

                        clearForm();
                        navigateToHome();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        cancelTimeout();
                        Log.e(TAG, "saveTicketToFirebase: failed — " + errorMessage);

                        // ALWAYS stop loading on failure
                        stopLoading();
                        showError("Failed to save ticket: " + errorMessage);
                    }
                });
    }

    // ── Timeout helpers ───────────────────────────────────────────────────────

    /**
     * Starts a safety timeout. If neither success nor failure fires within
     * UPLOAD_TIMEOUT_MS, the loading is stopped and an error is shown.
     * Calling startTimeout() again replaces any existing timeout.
     */
    private void startTimeout(String timeoutMessage) {
        cancelTimeout();
        timeoutRunnable = () -> {
            if (!isAdded()) return;
            Log.e(TAG, "TIMEOUT: " + timeoutMessage);
            stopLoading();
            showError(timeoutMessage);
        };
        timeoutHandler.postDelayed(timeoutRunnable, UPLOAD_TIMEOUT_MS);
    }

    /** Cancels the pending timeout (call when a callback fires normally). */
    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToHome();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean valid = true;
        tvTicketUploadError.setVisibility(View.GONE);

        if (etEventName.getText().toString().trim().isEmpty()) {
            etEventName.setError("Event name is required");
            valid = false;
        }
        if (etDate.getText().toString().trim().isEmpty()) {
            etDate.setError("Date is required");
            valid = false;
        }
        String origStr = etOriginalPrice.getText().toString().trim();
        if (origStr.isEmpty()) {
            etOriginalPrice.setError("Original price is required");
            valid = false;
        } else {
            try {
                if (Integer.parseInt(origStr) <= 0) {
                    etOriginalPrice.setError("Enter a valid price");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etOriginalPrice.setError("Enter a valid price");
                valid = false;
            }
        }
        String sellStr = etSellingPrice.getText().toString().trim();
        if (sellStr.isEmpty()) {
            etSellingPrice.setError("Selling price is required");
            valid = false;
        } else {
            try {
                if (Integer.parseInt(sellStr) <= 0) {
                    etSellingPrice.setError("Enter a valid price");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etSellingPrice.setError("Enter a valid price");
                valid = false;
            }
        }
        if (selectedTicketFileUri == null) {
            tvTicketUploadError.setVisibility(View.VISIBLE);
            layoutTicketFilePicker.setBackground(
                    requireContext().getDrawable(R.drawable.bg_upload_area_error));
            valid = false;
        }
        return valid;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Stops all loading indicators and re-enables the submit button.
     * Called in EVERY success and failure path — no exceptions.
     */
    private void stopLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        setSubmitLoading(false);
    }

    private void setSubmitLoading(boolean loading) {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!loading);
            btnSubmit.setAlpha(loading ? 0.6f : 1.0f);
        }
    }

    private void clearForm() {
        etEventName.setText("");
        etDate.setText("");
        etOriginalPrice.setText("");
        etSellingPrice.setText("");
        tvRecoveryMessage.setText(getString(R.string.enter_selling_price));
        tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.text_secondary));

        selectedEventImageUri = null;
        ivEventImagePreview.setImageDrawable(null);
        frameEventImage.setVisibility(View.GONE);
        layoutEventImagePicker.setVisibility(View.VISIBLE);

        selectedTicketFileUri = null;
        frameTicketFile.setVisibility(View.GONE);
        layoutTicketFilePicker.setVisibility(View.VISIBLE);
        layoutTicketFilePicker.setBackground(
                requireContext().getDrawable(R.drawable.bg_upload_area));
        tvTicketUploadError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (rootView != null && isAdded()) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(requireContext().getColor(R.color.error_red))
                    .setTextColor(requireContext().getColor(R.color.white))
                    .show();
        }
    }

    private int parsePrice(EditText et) {
        try {
            return Integer.parseInt(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) return path.substring(cut + 1);
        }
        return "ticket_file";
    }
}
