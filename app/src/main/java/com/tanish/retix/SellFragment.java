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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SellFragment extends Fragment {

    // ── Form fields ───────────────────────────────────────────────────────────
    private EditText etEventName, etDate, etOriginalPrice, etSellingPrice;
    private TextView tvRecoveryMessage;
    private MaterialButton btnSubmit;
    private View rootView;

    // ── Event image upload ────────────────────────────────────────────────────
    private LinearLayout layoutEventImagePicker;   // empty-state tap area
    private FrameLayout  frameEventImage;          // preview container
    private ImageView    ivEventImagePreview;
    private MaterialButton btnChangeEventImage, btnRemoveEventImage;

    // ── Ticket file upload ────────────────────────────────────────────────────
    private LinearLayout layoutTicketFilePicker;   // empty-state tap area
    private LinearLayout frameTicketFile;          // preview row
    private TextView     tvTicketFileName;
    private TextView     tvTicketUploadError;      // validation error message
    private MaterialButton btnChangeTicketFile, btnRemoveTicketFile;

    // ── State ─────────────────────────────────────────────────────────────────
    private Uri selectedEventImageUri = null;
    private Uri selectedTicketFileUri = null;

    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private LoadingDialog loadingDialog;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable submitRunnable;

    // ── Activity result launchers ─────────────────────────────────────────────
    private ActivityResultLauncher<Intent> eventImageLauncher;
    private ActivityResultLauncher<Intent> ticketFileLauncher;

    public SellFragment() {}

    public static SellFragment newInstance() {
        return new SellFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        rootView     = view;
        loadingDialog = new LoadingDialog(requireContext());
        initViews(view);
        setupDatePicker();
        setupPricingListener();
        setupUploadButtons();
        setupSubmitButton();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews(View view) {
        etEventName       = view.findViewById(R.id.et_event_name);
        etDate            = view.findViewById(R.id.et_date);
        etOriginalPrice   = view.findViewById(R.id.et_original_price);
        etSellingPrice    = view.findViewById(R.id.et_selling_price);
        tvRecoveryMessage = view.findViewById(R.id.tv_recovery_message);
        btnSubmit         = view.findViewById(R.id.btn_submit);

        // Event image
        layoutEventImagePicker = view.findViewById(R.id.layout_event_image_picker);
        frameEventImage        = view.findViewById(R.id.frame_event_image);
        ivEventImagePreview    = view.findViewById(R.id.iv_event_image_preview);
        btnChangeEventImage    = view.findViewById(R.id.btn_change_event_image);
        btnRemoveEventImage    = view.findViewById(R.id.btn_remove_event_image);

        // Ticket file
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
        // ── Event image ──
        layoutEventImagePicker.setOnClickListener(v -> pickEventImage());

        btnChangeEventImage.setOnClickListener(v -> pickEventImage());

        btnRemoveEventImage.setOnClickListener(v -> {
            selectedEventImageUri = null;
            ivEventImagePreview.setImageDrawable(null);
            frameEventImage.setVisibility(View.GONE);
            layoutEventImagePicker.setVisibility(View.VISIBLE);
        });

        // ── Ticket file ──
        layoutTicketFilePicker.setOnClickListener(v -> pickTicketFile());

        btnChangeTicketFile.setOnClickListener(v -> pickTicketFile());

        btnRemoveTicketFile.setOnClickListener(v -> {
            selectedTicketFileUri = null;
            frameTicketFile.setVisibility(View.GONE);
            layoutTicketFilePicker.setVisibility(View.VISIBLE);
            // Restore normal border
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

    // ── Selection callbacks ───────────────────────────────────────────────────

    private void onEventImageSelected(Uri uri) {
        selectedEventImageUri = uri;
        ivEventImagePreview.setImageURI(uri);
        // Show preview, hide picker
        frameEventImage.setVisibility(View.VISIBLE);
        layoutEventImagePicker.setVisibility(View.GONE);
    }

    private void onTicketFileSelected(Uri uri) {
        selectedTicketFileUri = uri;
        tvTicketFileName.setText(getFileName(uri));
        // Show file row, hide picker, clear any error
        frameTicketFile.setVisibility(View.VISIBLE);
        layoutTicketFilePicker.setVisibility(View.GONE);
        tvTicketUploadError.setVisibility(View.GONE);
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                loadingDialog.setMessage("Listing your ticket...");
                loadingDialog.show();
                submitRunnable = () -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    showSuccess("Ticket listed successfully!");
                    clearForm();
                };
                mainHandler.postDelayed(submitRunnable, 1500);
            }
        });
    }

    private void showSuccess(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(requireContext().getColor(R.color.success_green))
                    .setTextColor(requireContext().getColor(R.color.white))
                    .show();
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        // Clear previous ticket upload error
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
                if (Integer.parseInt(origStr) <= 0) { etOriginalPrice.setError("Enter a valid price"); valid = false; }
            } catch (NumberFormatException e) { etOriginalPrice.setError("Enter a valid price"); valid = false; }
        }
        String sellStr = etSellingPrice.getText().toString().trim();
        if (sellStr.isEmpty()) {
            etSellingPrice.setError("Selling price is required");
            valid = false;
        } else {
            try {
                if (Integer.parseInt(sellStr) <= 0) { etSellingPrice.setError("Enter a valid price"); valid = false; }
            } catch (NumberFormatException e) { etSellingPrice.setError("Enter a valid price"); valid = false; }
        }

        // Ticket file is MANDATORY
        if (selectedTicketFileUri == null) {
            tvTicketUploadError.setVisibility(View.VISIBLE);
            // Scroll hint: highlight the picker area border
            layoutTicketFilePicker.setBackground(
                    requireContext().getDrawable(R.drawable.bg_upload_area_error));
            valid = false;
        }

        return valid;
    }

    private void clearForm() {
        etEventName.setText("");
        etDate.setText("");
        etOriginalPrice.setText("");
        etSellingPrice.setText("");
        tvRecoveryMessage.setText(getString(R.string.enter_selling_price));
        tvRecoveryMessage.setTextColor(requireContext().getColor(R.color.text_secondary));

        // Reset event image
        selectedEventImageUri = null;
        ivEventImagePreview.setImageDrawable(null);
        frameEventImage.setVisibility(View.GONE);
        layoutEventImagePicker.setVisibility(View.VISIBLE);

        // Reset ticket file
        selectedTicketFileUri = null;
        frameTicketFile.setVisibility(View.GONE);
        layoutTicketFilePicker.setVisibility(View.VISIBLE);
        layoutTicketFilePicker.setBackground(
                requireContext().getDrawable(R.drawable.bg_upload_area));
        tvTicketUploadError.setVisibility(View.GONE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) return path.substring(cut + 1);
        }
        return "ticket_file";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending callbacks and dismiss dialog to prevent leaks
        if (submitRunnable != null) mainHandler.removeCallbacks(submitRunnable);
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}
