package com.tanish.retix;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements TicketAdapter.OnTicketClickListener {

    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private EditText etSearch;
    private View layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle, tvEmptyIcon;
    private FloatingActionButton fabSellTicket;
    private ImageButton btnSettings;

    private List<Ticket> tickets;
    private OnFragmentInteractionListener listener;

    public interface OnFragmentInteractionListener {
        void onSellTicketClick();
        void onSettingsFromHomeClick();
        void onTicketClick(Ticket ticket);
    }

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFab();
        setupSettingsButton();
        loadDummyData();
    }

    private void initViews(View view) {
        rvTickets        = view.findViewById(R.id.rv_tickets);
        etSearch         = view.findViewById(R.id.et_search);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyTitle     = layoutEmptyState.findViewById(R.id.tv_empty_title);
        tvEmptySubtitle  = layoutEmptyState.findViewById(R.id.tv_empty_subtitle);
        tvEmptyIcon      = layoutEmptyState.findViewById(R.id.tv_empty_icon);
        fabSellTicket    = view.findViewById(R.id.fab_sell_ticket);
        btnSettings      = view.findViewById(R.id.btn_settings);
    }

    private void setupRecyclerView() {
        tickets = new ArrayList<>();
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        ticketAdapter = new TicketAdapter(tickets, this);
        rvTickets.setAdapter(ticketAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ticketAdapter.filter(s.toString());
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        fabSellTicket.setOnClickListener(v -> {
            if (listener != null) listener.onSellTicketClick();
        });
    }

    private void setupSettingsButton() {
        btnSettings.setOnClickListener(v -> {
            if (listener != null) listener.onSettingsFromHomeClick();
        });
    }

    private void loadDummyData() {
        tickets = new ArrayList<>();

        // No resId passed — smart keyword detection picks the right image
        tickets.add(new Ticket("Coldplay: Music of the Spheres",
                "Sat, Jan 18 • 7:00 PM", 7500, 5500, "Arjun Sharma", 4.8f));

        tickets.add(new Ticket("Ed Sheeran: +-=÷x Tour",
                "Sun, Feb 2 • 6:30 PM", 5000, 7200, "Priya Patel", 4.9f));

        tickets.add(new Ticket("Arijit Singh Live Concert",
                "Fri, Jan 24 • 8:00 PM", 3500, 2800, "Rahul Verma", 4.6f));

        tickets.add(new Ticket("IPL 2025: RCB vs CSK",
                "Sat, Mar 15 • 3:30 PM", 2500, 2500, "Karan Malhotra", 4.7f));

        tickets.add(new Ticket("Sunburn Goa 2025",
                "Dec 27-29 • 4:00 PM", 4500, 3200, "Neha Gupta", 4.5f));

        tickets.add(new Ticket("Zakir Khan: Haq Se Single",
                "Sat, Feb 8 • 7:30 PM", 1500, 1200, "Sneha Joshi", 4.7f));

        tickets.add(new Ticket("Hamlet – Shakespeare Drama",
                "Sun, Feb 16 • 6:00 PM", 1200, 900, "Vikram Nair", 4.4f));

        tickets.add(new Ticket("Google DevFest 2025",
                "Sat, Mar 1 • 10:00 AM", 500, 400, "Ananya Singh", 4.6f));

        tickets.add(new Ticket("Mumbai Street Food Festival",
                "Sat, Mar 22 • 12:00 PM", 300, 250, "Rohan Mehta", 4.3f));

        ticketAdapter.updateTickets(tickets);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (ticketAdapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTickets.setVisibility(View.GONE);
            tvEmptyIcon.setText("🎫");
            tvEmptyTitle.setText("No tickets available");
            tvEmptySubtitle.setText("Check back later or list your own ticket to get started");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTickets.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        if (listener != null) listener.onTicketClick(ticket);
    }

    public void refreshData() {
        loadDummyData();
    }
}
