package com.tanish.retix;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class TicketDetailActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "ReTixPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ImageView ivEventImage;
    private TextView tvEventName, tvDate, tvSellerName, tvSellerInitial, tvRating;
    private TextView tvOriginalPrice, tvSellingPrice, tvPricingBadge;
    private ImageButton btnBack;
    private MaterialButton btnBuy, btnConfirmEntry, btnViewTicket, btnChat;
    private View rootView;
    private LoadingDialog loadingDialog;

    private Ticket ticket;
    private boolean isPurchased = false;
    private boolean isEntryConfirmed = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable purchaseRunnable;
    private Runnable confirmEntryRunnable;

    // Rating dialog views
    private ImageView star1, star2, star3, star4, star5;
    private TextView tvRatingLabel;
    private int selectedRating = 0;
    private View ratingDialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        loadingDialog = new LoadingDialog(this);
        initViews();
        rootView = findViewById(android.R.id.content);

        ticket = getIntent().getParcelableExtra("ticket");
        if (ticket == null) {
            Toast.makeText(this, "Ticket not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindData();
        setupListeners();
    }

    private void initViews() {
        ivEventImage    = findViewById(R.id.iv_event_image);
        tvEventName     = findViewById(R.id.tv_event_name);
        tvDate          = findViewById(R.id.tv_date);
        tvSellerName    = findViewById(R.id.tv_seller_name);
        tvSellerInitial = findViewById(R.id.tv_seller_initial);
        tvRating        = findViewById(R.id.tv_rating);
        tvOriginalPrice = findViewById(R.id.tv_original_price);
        tvSellingPrice  = findViewById(R.id.tv_selling_price);
        tvPricingBadge  = findViewById(R.id.tv_pricing_badge);
        btnBack         = findViewById(R.id.btn_back);
        btnBuy          = findViewById(R.id.btn_buy);
        btnConfirmEntry = findViewById(R.id.btn_confirm_entry);
        btnViewTicket   = findViewById(R.id.btn_view_ticket);
        btnChat         = findViewById(R.id.btn_chat);
    }

    private void bindData() {
        // Event image
        if (ticket.getEventImageUri() != null && !ticket.getEventImageUri().isEmpty()) {
            try {
                ivEventImage.setImageURI(Uri.parse(ticket.getEventImageUri()));
            } catch (Exception e) {
                setPlaceholderImage();
            }
        } else if (ticket.getEventImageResId() != 0) {
            ivEventImage.setImageResource(ticket.getEventImageResId());
        } else {
            setPlaceholderImage();
        }

        tvEventName.setText(ticket.getEventName());
        tvDate.setText(ticket.getDate());

        // Seller info
        tvSellerName.setText(ticket.getSellerName());
        if (ticket.getSellerName() != null && !ticket.getSellerName().isEmpty()) {
            tvSellerInitial.setText(
                    String.valueOf(ticket.getSellerName().charAt(0)).toUpperCase());
        }
        tvRating.setText(String.format("%.1f", ticket.getRating()));

        // Pricing
        tvOriginalPrice.setText("₹" + ticket.getOriginalPrice());
        tvSellingPrice.setText("₹" + ticket.getSellingPrice());

        if (ticket.isDiscounted()) {
            int savings = ticket.getSavings();
            tvPricingBadge.setText(getString(R.string.you_save, savings));
            tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge);
            tvPricingBadge.setTextColor(getColor(R.color.success_green));
            tvOriginalPrice.setPaintFlags(
                    tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else if (ticket.getSellingPrice() > ticket.getOriginalPrice()) {
            tvPricingBadge.setText(getString(R.string.limited_availability));
            tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge_urgent);
            tvPricingBadge.setTextColor(getColor(android.R.color.holo_orange_dark));
            tvOriginalPrice.setPaintFlags(
                    tvOriginalPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvPricingBadge.setText(getString(R.string.same_price));
            tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge);
            tvPricingBadge.setTextColor(getColor(R.color.text_secondary));
            tvOriginalPrice.setPaintFlags(
                    tvOriginalPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void setPlaceholderImage() {
        ivEventImage.setImageResource(R.drawable.bg_event_placeholder);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        });

        // Seller name → seller profile
        tvSellerName.setOnClickListener(v -> {
            Intent intent = new Intent(this, SellerProfileActivity.class);
            intent.putExtra("seller_name", ticket.getSellerName());
            intent.putExtra("seller_rating", ticket.getRating());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // Chat with seller
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("seller_name", ticket.getSellerName());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // View Ticket (PDF/image)
        btnViewTicket.setOnClickListener(v -> openTicketFile());

        // Confirm Entry
        btnConfirmEntry.setOnClickListener(v -> showConfirmEntryDialog());

        // Buy / main action
        btnBuy.setOnClickListener(v -> {
            if (!isPurchased) {
                showBuyConfirmationDialog();
            }
        });
    }

    // ─── Buy Flow ────────────────────────────────────────────────────────────

    private void showBuyConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_purchase)
                .setMessage(R.string.confirm_buy_message)
                .setPositiveButton(R.string.confirm, (d, w) -> processPurchase())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void processPurchase() {
        loadingDialog.setMessage("Processing your payment...");
        loadingDialog.show();

        purchaseRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            loadingDialog.dismiss();
            isPurchased = true;

            // Update UI state
            btnBuy.setText("Payment Processing…");
            btnBuy.setEnabled(false);
            btnBuy.setAlpha(0.6f);
            btnConfirmEntry.setVisibility(View.VISIBLE);

            // Show ticket file button if ticket has a file
            if (ticket.hasTicketFile()) {
                btnViewTicket.setVisibility(View.VISIBLE);
            }

            Snackbar.make(rootView, R.string.payment_processing_message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.accent_blue))
                    .setTextColor(getColor(R.color.white))
                    .show();
        };
        mainHandler.postDelayed(purchaseRunnable, 1500);
    }

    // ─── Confirm Entry Flow ───────────────────────────────────────────────────

    private void showConfirmEntryDialog() {
        if (isEntryConfirmed) {
            Toast.makeText(this, R.string.payment_available_message, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_entry)
                .setMessage(R.string.confirm_entry_message)
                .setPositiveButton(R.string.confirm, (d, w) -> confirmEntry())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmEntry() {
        loadingDialog.setMessage("Confirming entry...");
        loadingDialog.show();

        confirmEntryRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            loadingDialog.dismiss();
            isEntryConfirmed = true;

            // Update confirm entry button
            btnConfirmEntry.setText("✓ Entry Confirmed");
            btnConfirmEntry.setEnabled(false);
            btnConfirmEntry.setAlpha(0.7f);

            Snackbar.make(rootView, R.string.payment_available_message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.success_green))
                    .setTextColor(getColor(R.color.white))
                    .show();

            showRatingDialog();
        };
        mainHandler.postDelayed(confirmEntryRunnable, 1200);
    }

    // ─── View Ticket File ─────────────────────────────────────────────────────

    private void openTicketFile() {
        String fileUri = ticket.getTicketFileUri();
        if (fileUri == null || fileUri.isEmpty()) {
            Toast.makeText(this, "No ticket file attached", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(fileUri);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Ticket"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open ticket file", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Rating Dialog ────────────────────────────────────────────────────────

    private void showRatingDialog() {
        ratingDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null);

        star1 = ratingDialogView.findViewById(R.id.star_1);
        star2 = ratingDialogView.findViewById(R.id.star_2);
        star3 = ratingDialogView.findViewById(R.id.star_3);
        star4 = ratingDialogView.findViewById(R.id.star_4);
        star5 = ratingDialogView.findViewById(R.id.star_5);
        tvRatingLabel = ratingDialogView.findViewById(R.id.tv_rating_label);

        MaterialButton btnSubmitRating = ratingDialogView.findViewById(R.id.btn_submit_rating);
        setupStarRating();

        AlertDialog ratingDialog = new AlertDialog.Builder(this)
                .setView(ratingDialogView)
                .setCancelable(false)
                .create();

        if (ratingDialog.getWindow() != null) {
            ratingDialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnSubmitRating.setOnClickListener(v -> {
            if (selectedRating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            submitRating(selectedRating);
            ratingDialog.dismiss();
        });

        ratingDialog.show();
    }

    private void setupStarRating() {
        View.OnClickListener starClick = v -> {
            int r;
            if      (v.getId() == R.id.star_1) r = 1;
            else if (v.getId() == R.id.star_2) r = 2;
            else if (v.getId() == R.id.star_3) r = 3;
            else if (v.getId() == R.id.star_4) r = 4;
            else                               r = 5;
            selectedRating = r;
            updateStarVisuals(r);
            updateRatingLabel(r);
        };
        star1.setOnClickListener(starClick);
        star2.setOnClickListener(starClick);
        star3.setOnClickListener(starClick);
        star4.setOnClickListener(starClick);
        star5.setOnClickListener(starClick);
    }

    private void updateStarVisuals(int rating) {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                stars[i].setColorFilter(i < rating
                        ? getColor(R.color.accent_blue)
                        : getColor(R.color.text_hint));
            }
        }
    }

    private void updateRatingLabel(int rating) {
        String[] labels = {"", getString(R.string.very_poor), getString(R.string.poor),
                getString(R.string.average), getString(R.string.good), getString(R.string.excellent)};
        tvRatingLabel.setText(labels[rating]);
    }

    private void submitRating(int rating) {
        Toast.makeText(this, R.string.thank_you_feedback, Toast.LENGTH_SHORT).show();
        btnBuy.setText(R.string.payment_released);
        btnBuy.setEnabled(false);
        btnBuy.setAlpha(0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending delayed callbacks to prevent leaks/crashes
        if (purchaseRunnable != null) mainHandler.removeCallbacks(purchaseRunnable);
        if (confirmEntryRunnable != null) mainHandler.removeCallbacks(confirmEntryRunnable);
        // Dismiss dialog if still showing to avoid WindowLeaked exception
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}
