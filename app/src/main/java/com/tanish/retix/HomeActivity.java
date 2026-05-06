package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements TicketAdapter.OnTicketClickListener {

    private static final String PREFS_NAME = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private EditText etSearch;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabSellTicket;

    private List<Ticket> tickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();
        loadTicketsFromApi();

        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void initViews() {
        rvTickets = findViewById(R.id.rv_tickets);
        etSearch = findViewById(R.id.et_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        fabSellTicket = findViewById(R.id.fab_sell_ticket);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Profile button click
        findViewById(R.id.btn_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        tickets = new ArrayList<>();
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new TicketAdapter(tickets, this);
        rvTickets.setAdapter(ticketAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ticketAdapter.filter(s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        fabSellTicket.setOnClickListener(v -> {
            Intent intent = new Intent(this, SellTicketActivity.class);
            startActivity(intent);
        });
    }

    private void loadTicketsFromApi() {
        ApiManager apiManager = ApiManager.getInstance();
        apiManager.fetchAvailableTickets(new ApiManager.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> result) {
                tickets = result;
                ticketAdapter.updateTickets(tickets);
                updateEmptyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                tickets = new ArrayList<>();
                ticketAdapter.updateTickets(tickets);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (ticketAdapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTickets.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTickets.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("ticket", ticket);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
