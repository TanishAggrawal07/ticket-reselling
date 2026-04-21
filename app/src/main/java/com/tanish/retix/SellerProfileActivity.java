package com.tanish.retix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SellerProfileActivity extends AppCompatActivity implements TicketAdapter.OnTicketClickListener {

    private static final String PREFS_NAME    = "ReTixPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ImageButton btnBack;
    private TextView tvSellerName, tvSellerInitial, tvSellerRating, tvListingsTitle;
    private RecyclerView rvSellerTickets;
    private View layoutEmptyListings;

    private TicketAdapter ticketAdapter;
    private List<Ticket> sellerTickets;

    private String sellerName;
    private float sellerRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        // Apply transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        // Get seller info from intent
        sellerName = getIntent().getStringExtra("seller_name");
        sellerRating = getIntent().getFloatExtra("seller_rating", 4.5f);

        initViews();
        setupToolbar();
        setupRecyclerView();
        displaySellerInfo();
        loadSellerTickets();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvSellerName = findViewById(R.id.tv_seller_name);
        tvSellerInitial = findViewById(R.id.tv_seller_initial);
        tvSellerRating = findViewById(R.id.tv_seller_rating);
        tvListingsTitle = findViewById(R.id.tv_listings_title);
        rvSellerTickets = findViewById(R.id.rv_seller_tickets);
        layoutEmptyListings = findViewById(R.id.layout_empty_listings);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        sellerTickets = new ArrayList<>();
        rvSellerTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new TicketAdapter(sellerTickets, this);
        rvSellerTickets.setAdapter(ticketAdapter);
    }

    private void displaySellerInfo() {
        if (sellerName == null || sellerName.isEmpty()) {
            sellerName = "Unknown Seller";
        }
        tvSellerName.setText(sellerName);
        tvSellerInitial.setText(String.valueOf(sellerName.charAt(0)).toUpperCase());
        tvSellerRating.setText(String.format("%.1f", sellerRating));
        tvListingsTitle.setText("Listings by " + sellerName);
    }

    private void loadSellerTickets() {
        sellerTickets = new ArrayList<>();

        // Load dummy tickets for this seller
        // In a real app, filter tickets by seller name from database
        sellerTickets.add(new Ticket(
                "Coldplay: Music of the Spheres",
                "Sat, Jan 18 • 7:00 PM",
                7500,
                5500,
                sellerName,
                sellerRating
        ));

        sellerTickets.add(new Ticket(
                "Imagine Dragons: Mercury Tour",
                "Thu, Feb 13 • 7:30 PM",
                6000,
                4800,
                sellerName,
                sellerRating
        ));

        sellerTickets.add(new Ticket(
                "The Weeknd: After Hours Tour",
                "Sat, Mar 8 • 8:00 PM",
                8000,
                6500,
                sellerName,
                sellerRating
        ));

        ticketAdapter.updateTickets(sellerTickets);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (sellerTickets.isEmpty()) {
            rvSellerTickets.setVisibility(android.view.View.GONE);
            layoutEmptyListings.setVisibility(android.view.View.VISIBLE);
            // Configure empty state text
            android.widget.TextView iconView = layoutEmptyListings.findViewById(R.id.tv_empty_icon);
            android.widget.TextView titleView = layoutEmptyListings.findViewById(R.id.tv_empty_title);
            android.widget.TextView subtitleView = layoutEmptyListings.findViewById(R.id.tv_empty_subtitle);
            if (iconView != null) iconView.setText("🎫");
            if (titleView != null) titleView.setText("No listings yet");
            if (subtitleView != null) subtitleView.setText("This seller has no active listings");
        } else {
            rvSellerTickets.setVisibility(android.view.View.VISIBLE);
            layoutEmptyListings.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        // Navigate to ticket detail
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("ticket", ticket);
        startActivity(intent);
    }
}
