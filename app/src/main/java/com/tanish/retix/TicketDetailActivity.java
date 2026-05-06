package com.tanish.retix;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class TicketDetailActivity extends AppCompatActivity {

    private ImageView ivEventImage;
    private TextView tvEventName, tvDate, tvSellerName, tvSellerInitial, tvRating;
    private TextView tvOriginalPrice, tvSellingPrice, tvPricingBadge;
    private TextView tvWalletBalanceHint;
    private ImageButton btnBack;
    private MaterialButton btnBuy, btnViewTicket, btnChat, btnChatWithBuyer;
    private View rootView;
    private LoadingDialog loadingDialog;

    private Ticket ticket;
    private boolean isPurchased = false;
    private boolean isViewingPurchasedTicket = false;
    private long currentWalletBalance = -1;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

        // Check if viewing a purchased ticket (from My Tickets tab)
        isViewingPurchasedTicket = getIntent().getBooleanExtra("is_purchased", false);

        // Check if viewing own ticket
        String currentUserId = ApiClient.getTokenManager().getUserId();
        boolean isOwnTicket = currentUserId != null && currentUserId.equals(ticket.getSellerId());

        bindData();
        setupListeners();
        loadWalletBalance();

        // Hide Buy and Chat buttons for own tickets
        if (isOwnTicket) {
            btnBuy.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);

            // Show "Your Listing" badge or message
            TextView tvOwnTicketBadge = findViewById(R.id.tv_own_ticket_badge);
            if (tvOwnTicketBadge != null) {
                tvOwnTicketBadge.setVisibility(View.VISIBLE);
            }

            // Show View Ticket for seller's own listings with a file
            if (ticket.hasTicketFile()) {
                btnViewTicket.setVisibility(View.VISIBLE);
            }

            // If ticket is sold, show "Chat with Buyer" button
            if (ticket.getStatus() != null && ticket.getStatus().equals(Ticket.STATUS_SOLD)
                    && ticket.getBuyerId() != null && !ticket.getBuyerId().isEmpty()) {
                btnChatWithBuyer.setVisibility(View.VISIBLE);
            }
        }

        // For purchased tickets, show "Purchased" state with View Ticket button
        if (isViewingPurchasedTicket) {
            isPurchased = true;
            btnBuy.setText("Purchased");
            btnBuy.setEnabled(false);
            btnBuy.setAlpha(0.5f);
            btnChat.setVisibility(View.GONE);
            if (ticket.hasTicketFile()) {
                btnViewTicket.setVisibility(View.VISIBLE);
            }
        }
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
        btnViewTicket   = findViewById(R.id.btn_view_ticket);
        btnChat         = findViewById(R.id.btn_chat);
        btnChatWithBuyer = findViewById(R.id.btn_chat_with_buyer);
        tvWalletBalanceHint = findViewById(R.id.tv_wallet_balance_hint);
    }

    private void bindData() {
        // Event image — priority: Remote URL → local URI → drawable → placeholder
        String imageUrl = ticket.getEventImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImageFromStorage(imageUrl);
        } else if (ticket.getEventImageUri() != null && !ticket.getEventImageUri().isEmpty()) {
            try {
                ivEventImage.setImageURI(Uri.parse(ticket.getEventImageUri()));
            } catch (Exception e) {
                setPlaceholderImage();
            }
        } else if (ticket.getEventImageResId() != 0) {
            ivEventImage.setImageResource(ticket.getEventImageResId());
        } else {
            ivEventImage.setImageResource(ticket.getSmartImageResId());
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

    private void loadImageFromStorage(String url) {
        // HTTPS URLs (Cloudinary or any other) — load them on a background thread
        if (url.startsWith("http://") || url.startsWith("https://")) {
            new Thread(() -> {
                try {
                    java.net.URL imageUrl = new java.net.URL(url);
                    java.net.HttpURLConnection connection =
                            (java.net.HttpURLConnection) imageUrl.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    android.graphics.Bitmap bmp =
                            android.graphics.BitmapFactory.decodeStream(input);
                    input.close();
                    if (bmp != null) {
                        runOnUiThread(() -> ivEventImage.setImageBitmap(bmp));
                    } else {
                        runOnUiThread(() ->
                                ivEventImage.setImageResource(ticket.getSmartImageResId()));
                    }
                } catch (Exception e) {
                    runOnUiThread(() ->
                            ivEventImage.setImageResource(ticket.getSmartImageResId()));
                }
            }).start();
        } else {
            setPlaceholderImage();
        }
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
            intent.putExtra("seller_id",   ticket.getSellerId());
            intent.putExtra("seller_rating", ticket.getRating());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // Chat with seller — pass both name and sellerId
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiver_name", ticket.getSellerName());
            intent.putExtra("receiver_id", ticket.getSellerId());
            // Legacy support
            intent.putExtra("seller_name", ticket.getSellerName());
            intent.putExtra("seller_id", ticket.getSellerId());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // Chat with buyer — for sellers to chat with buyers
        if (btnChatWithBuyer != null) {
            btnChatWithBuyer.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiver_id", ticket.getBuyerId());
                intent.putExtra("receiver_name", "Buyer"); // Could fetch buyer name from API
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

        // View Ticket (PDF/image)
        btnViewTicket.setOnClickListener(v -> openTicketFile());

        // Buy / main action
        btnBuy.setOnClickListener(v -> {
            if (!isPurchased) {
                showBuyConfirmationDialog();
            }
        });
    }

    // ─── Buy Flow ────────────────────────────────────────────────────────────

    private void showBuyConfirmationDialog() {
        // Pre-check wallet balance
        if (currentWalletBalance >= 0 && currentWalletBalance < ticket.getSellingPrice()) {
            new AlertDialog.Builder(this)
                    .setTitle("Insufficient Balance")
                    .setMessage("Your wallet balance (₹" + currentWalletBalance
                            + ") is less than the ticket price (₹" + ticket.getSellingPrice()
                            + "). Please add money to your wallet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String balanceInfo = currentWalletBalance >= 0
                ? "\n\nWallet Balance: ₹" + currentWalletBalance : "";

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_purchase)
                .setMessage(getString(R.string.confirm_buy_message) + balanceInfo)
                .setPositiveButton(R.string.confirm, (d, w) -> processPurchase())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadWalletBalance() {
        String uid = ApiClient.getTokenManager().getUserId();
        if (uid == null) return;
        ApiManager.getInstance().getWalletBalance(uid, new ApiManager.BalanceCallback() {
            @Override
            public void onSuccess(long balance, long pendingBalance) {
                if (isFinishing() || isDestroyed()) return;
                currentWalletBalance = balance;
                if (tvWalletBalanceHint != null) {
                    tvWalletBalanceHint.setText("Wallet: ₹" + balance);
                    tvWalletBalanceHint.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                if (tvWalletBalanceHint != null) {
                    tvWalletBalanceHint.setVisibility(View.GONE);
                }
            }
        });
    }

    private void processPurchase() {
        loadingDialog.setMessage("Processing your payment...");
        loadingDialog.show();

        // Use API to purchase ticket
        ApiManager.getInstance().buyTicket(ticket.getFirestoreId(), new ApiManager.VoidCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                loadingDialog.dismiss();
                isPurchased = true;

                btnBuy.setText("Purchased");
                btnBuy.setEnabled(false);
                btnBuy.setAlpha(0.5f);

                // Show ticket file button if ticket has a file
                if (ticket.hasTicketFile()) {
                    btnViewTicket.setVisibility(View.VISIBLE);
                }

                Snackbar.make(rootView, "Ticket purchased successfully! You can view your ticket below.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.success_green))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                loadingDialog.dismiss();

                // Show user-friendly error messages
                String userMessage;
                if (errorMessage.contains("INSUFFICIENT_BALANCE")) {
                    userMessage = "Insufficient wallet balance. Please add money to your wallet.";
                } else if (errorMessage.contains("TICKET_NOT_AVAILABLE")) {
                    userMessage = "This ticket is no longer available.";
                } else if (errorMessage.contains("OWN_TICKET")) {
                    userMessage = "You cannot buy your own ticket.";
                } else {
                    userMessage = "Purchase failed: " + errorMessage;
                }

                Snackbar.make(rootView, userMessage, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.error_red))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }
        });
    }

    // ─── View Ticket File ─────────────────────────────────────────────────────

    private void openTicketFile() {
        // Prefer the remote Firebase Storage URL; fall back to local URI
        String fileRef = ticket.getBestTicketFileRef();
        if (fileRef == null || fileRef.isEmpty()) {
            Toast.makeText(this, "No ticket file attached", Toast.LENGTH_SHORT).show();
            return;
        }

        // If it's a Firebase Storage URL (starts with "https://"), open directly
        if (fileRef.startsWith("https://")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(fileRef));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open Ticket"));
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open ticket file", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Local content URI
        try {
            Uri uri = Uri.parse(fileRef);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Ticket"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open ticket file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}
