package com.tanish.retix;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class SellTicketActivity extends AppCompatActivity {

    private static final String TAG = "SellTicket";

    // Form fields
    private EditText etEventName, etDate, etOriginalPrice, etSellingPrice;
    private TextView tvRecoveryMessage;
    private MaterialButton btnSubmit;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    // Image upload views
    private FrameLayout layoutBannerUpload, layoutTicketUpload;
    private ImageView ivBannerPreview, ivTicketPreview;
    private LinearLayout layoutBannerPlaceholder, layoutTicketPlaceholder;
    private ImageButton btnRemoveBanner, btnRemoveTicket;

    // State
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private Uri bannerImageUri = null;
    private Uri ticketImageUri = null;
    private String uploadedBannerUrl = null;
    private String uploadedTicketUrl = null;

    // Activity result launchers
    private ActivityResultLauncher<Intent> bannerImageLauncher;
    private ActivityResultLauncher<Intent> ticketImageLauncher;

    private ApiManager apiManager;
    private int pendingUploads = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_ticket);

        apiManager = ApiManager.getInstance();

        initViews();
        setupImageLaunchers();
        setupToolbar();
        setupDatePicker();
        setupPricingListener();
        setupImageUploads();
        setupSubmitButton();
    }

    private void initViews() {
        // Form fields
        etEventName = findViewById(R.id.et_event_name);
        etDate = findViewById(R.id.et_date);
        etOriginalPrice = findViewById(R.id.et_original_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        tvRecoveryMessage = findViewById(R.id.tv_recovery_message);
        btnSubmit = findViewById(R.id.btn_submit);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);

        // Image upload views
        layoutBannerUpload = findViewById(R.id.layout_banner_upload);
        layoutTicketUpload = findViewById(R.id.layout_ticket_upload);
        ivBannerPreview = findViewById(R.id.iv_banner_preview);
        ivTicketPreview = findViewById(R.id.iv_ticket_preview);
        layoutBannerPlaceholder = findViewById(R.id.layout_banner_placeholder);
        layoutTicketPlaceholder = findViewById(R.id.layout_ticket_placeholder);
        btnRemoveBanner = findViewById(R.id.btn_remove_banner);
        btnRemoveTicket = findViewById(R.id.btn_remove_ticket);

        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault());
    }

    private void setupImageLaunchers() {
        bannerImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            bannerImageUri = uri;
                            showBannerImage(uri);
                        }
                    }
                });

        ticketImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            ticketImageUri = uri;
                            showTicketImage(uri);
                        }
                    }
                });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupImageUploads() {
        layoutBannerUpload.setOnClickListener(v -> pickImage(bannerImageLauncher));
        layoutTicketUpload.setOnClickListener(v -> pickImage(ticketImageLauncher));

        btnRemoveBanner.setOnClickListener(v -> removeBannerImage());
        btnRemoveTicket.setOnClickListener(v -> removeTicketImage());
    }

    private void pickImage(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        launcher.launch(intent);
    }

    private void showBannerImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivBannerPreview);
        ivBannerPreview.setVisibility(View.VISIBLE);
        layoutBannerPlaceholder.setVisibility(View.GONE);
        btnRemoveBanner.setVisibility(View.VISIBLE);
    }

    private void showTicketImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivTicketPreview);
        ivTicketPreview.setVisibility(View.VISIBLE);
        layoutTicketPlaceholder.setVisibility(View.GONE);
        btnRemoveTicket.setVisibility(View.VISIBLE);
    }

    private void removeBannerImage() {
        bannerImageUri = null;
        uploadedBannerUrl = null;
        ivBannerPreview.setVisibility(View.GONE);
        layoutBannerPlaceholder.setVisibility(View.VISIBLE);
        btnRemoveBanner.setVisibility(View.GONE);
    }

    private void removeTicketImage() {
        ticketImageUri = null;
        uploadedTicketUrl = null;
        ivTicketPreview.setVisibility(View.GONE);
        layoutTicketPlaceholder.setVisibility(View.VISIBLE);
        btnRemoveTicket.setVisibility(View.GONE);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        // Show time picker after date selection
                        showTimePicker();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            // Set minimum date to today
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void showTimePicker() {
        // For simplicity, set default time
        etDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void setupPricingListener() {
        TextWatcher pricingWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateRecoveryMessage();
            }
        };

        etSellingPrice.addTextChangedListener(pricingWatcher);
        etOriginalPrice.addTextChangedListener(pricingWatcher);
    }

    private void updateRecoveryMessage() {
        String sellingPriceStr = etSellingPrice.getText().toString().trim();
        String originalPriceStr = etOriginalPrice.getText().toString().trim();

        if (sellingPriceStr.isEmpty()) {
            tvRecoveryMessage.setText(getString(R.string.enter_selling_price));
            tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
            tvRecoveryMessage.setTextColor(getColor(R.color.text_secondary));
            return;
        }

        try {
            int sellingPrice = Integer.parseInt(sellingPriceStr);

            if (sellingPrice <= 0) {
                tvRecoveryMessage.setText("Please enter a valid price");
                tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                tvRecoveryMessage.setTextColor(getColor(R.color.error_red));
            } else if (!originalPriceStr.isEmpty()) {
                try {
                    int originalPrice = Integer.parseInt(originalPriceStr);
                    if (sellingPrice < originalPrice) {
                        int discount = originalPrice - sellingPrice;
                        tvRecoveryMessage.setText("Buyer saves ₹" + discount + " – competitive pricing!");
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                    } else if (sellingPrice > originalPrice) {
                        int premium = sellingPrice - originalPrice;
                        tvRecoveryMessage.setText("Premium ticket – recover ₹" + sellingPrice);
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.accent_blue));
                    } else {
                        tvRecoveryMessage.setText("Recover ₹" + sellingPrice + " from your ticket");
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                    }
                } catch (NumberFormatException e) {
                    tvRecoveryMessage.setText("Recover ₹" + sellingPrice + " from your ticket");
                    tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                    tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                }
            } else {
                tvRecoveryMessage.setText("Recover ₹" + sellingPrice + " from your ticket");
                tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
            }
        } catch (NumberFormatException e) {
            tvRecoveryMessage.setText("Please enter a valid price");
            tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
            tvRecoveryMessage.setTextColor(getColor(R.color.error_red));
        }
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitTicket();
            }
        });
    }

    private void submitTicket() {
        setLoading(true);

        // Upload images first if selected
        pendingUploads = 0;
        if (bannerImageUri != null) pendingUploads++;
        if (ticketImageUri != null) pendingUploads++;

        if (pendingUploads > 0) {
            if (bannerImageUri != null) {
                uploadImage(bannerImageUri, true);
            }
            if (ticketImageUri != null) {
                uploadImage(ticketImageUri, false);
            }
        } else {
            // No images to upload, create ticket directly
            createTicket();
        }
    }

    private void uploadImage(Uri imageUri, boolean isBanner) {
        new Thread(() -> {
            try {
                File tempFile = createTempFileFromUri(imageUri);
                if (tempFile == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        onUploadComplete();
                    });
                    return;
                }

                RequestBody requestFile = RequestBody.create(
                        MediaType.parse(getContentResolver().getType(imageUri) != null ?
                                getContentResolver().getType(imageUri) : "image/jpeg"),
                        tempFile
                );
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", tempFile.getName(), requestFile);

                runOnUiThread(() -> {
                    apiManager.uploadImage(imagePart, new ApiManager.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            if (isBanner) {
                                uploadedBannerUrl = imageUrl;
                            } else {
                                uploadedTicketUrl = imageUrl;
                            }
                            onUploadComplete();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "Image upload failed: " + errorMessage);
                            Toast.makeText(SellTicketActivity.this,
                                    "Image upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            onUploadComplete();
                        }
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    onUploadComplete();
                });
            }
        }).start();
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");

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

    private void onUploadComplete() {
        pendingUploads--;
        if (pendingUploads <= 0) {
            createTicket();
        }
    }

    private void createTicket() {
        String title = etEventName.getText().toString().trim();
        int originalPrice = Integer.parseInt(etOriginalPrice.getText().toString().trim());
        int sellingPrice = Integer.parseInt(etSellingPrice.getText().toString().trim());
        String eventDate = dateFormat.format(selectedDate.getTime());

        // Use uploaded image URL or empty string
        String imageUrl = uploadedBannerUrl != null ? uploadedBannerUrl : "";

        apiManager.saveTicket(title, "", originalPrice, sellingPrice, eventDate, imageUrl,
                "", // ticketFileUrl - not used in this flow
                ApiClient.getTokenManager().getUserId(),
                ApiClient.getTokenManager().getUserName(),
                new ApiManager.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(SellTicketActivity.this, "Ticket listed successfully", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(SellTicketActivity.this,
                                "Failed to list ticket: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate event name
        String eventName = etEventName.getText().toString().trim();
        if (eventName.isEmpty()) {
            etEventName.setError("Event name is required");
            isValid = false;
        }

        // Validate date
        String date = etDate.getText().toString().trim();
        if (date.isEmpty()) {
            etDate.setError("Date is required");
            isValid = false;
        }

        // Validate original price
        String originalPriceStr = etOriginalPrice.getText().toString().trim();
        if (originalPriceStr.isEmpty()) {
            etOriginalPrice.setError("Original price is required");
            isValid = false;
        } else {
            try {
                int originalPrice = Integer.parseInt(originalPriceStr);
                if (originalPrice <= 0) {
                    etOriginalPrice.setError("Please enter a valid price");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etOriginalPrice.setError("Please enter a valid price");
                isValid = false;
            }
        }

        // Validate selling price
        String sellingPriceStr = etSellingPrice.getText().toString().trim();
        if (sellingPriceStr.isEmpty()) {
            etSellingPrice.setError("Selling price is required");
            isValid = false;
        } else {
            try {
                int sellingPrice = Integer.parseInt(sellingPriceStr);
                if (sellingPrice <= 0) {
                    etSellingPrice.setError("Please enter a valid price");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etSellingPrice.setError("Please enter a valid price");
                isValid = false;
            }
        }

        return isValid;
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnSubmit.setEnabled(!loading);
        layoutBannerUpload.setEnabled(!loading);
        layoutTicketUpload.setEnabled(!loading);
        etEventName.setEnabled(!loading);
        etDate.setEnabled(!loading);
        etOriginalPrice.setEnabled(!loading);
        etSellingPrice.setEnabled(!loading);
    }
}
