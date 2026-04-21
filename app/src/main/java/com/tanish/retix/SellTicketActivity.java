package com.tanish.retix;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SellTicketActivity extends AppCompatActivity {

    private EditText etEventName, etDate, etOriginalPrice, etSellingPrice;
    private TextView tvRecoveryMessage;
    private MaterialButton btnSubmit;
    private Toolbar toolbar;

    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_ticket);

        initViews();
        setupToolbar();
        setupDatePicker();
        setupPricingListener();
        setupSubmitButton();
    }

    private void initViews() {
        etEventName = findViewById(R.id.et_event_name);
        etDate = findViewById(R.id.et_date);
        etOriginalPrice = findViewById(R.id.et_original_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        tvRecoveryMessage = findViewById(R.id.tv_recovery_message);
        btnSubmit = findViewById(R.id.btn_submit);
        toolbar = findViewById(R.id.toolbar);

        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
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
                        tvRecoveryMessage.setText("🟢 Buyer saves ₹" + discount + " – competitive pricing!");
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                    } else if (sellingPrice > originalPrice) {
                        int premium = sellingPrice - originalPrice;
                        tvRecoveryMessage.setText("💎 Premium ticket – recover ₹" + sellingPrice);
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.accent_blue));
                    } else {
                        tvRecoveryMessage.setText("🟢 Recover ₹" + sellingPrice + " from your ticket");
                        tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                        tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                    }
                } catch (NumberFormatException e) {
                    tvRecoveryMessage.setText("🟢 Recover ₹" + sellingPrice + " from your ticket");
                    tvRecoveryMessage.setBackgroundResource(R.drawable.bg_pricing_badge);
                    tvRecoveryMessage.setTextColor(getColor(R.color.success_green));
                }
            } else {
                tvRecoveryMessage.setText("🟢 Recover ₹" + sellingPrice + " from your ticket");
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
                // Show success message
                Toast.makeText(this, "Ticket listed successfully", Toast.LENGTH_LONG).show();
                // Finish activity after short delay
                finish();
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
}
