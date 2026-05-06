package com.tanish.retix;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment - Displays available tickets using the API.
 */
public class HomeFragment extends Fragment implements TicketAdapter.OnTicketClickListener {

    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private EditText etSearch;
    private View layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private ImageView ivEmptyIcon;
    private FloatingActionButton fabSellTicket;
    private View skeletonContainer;

    private List<Ticket> tickets;
    private OnFragmentInteractionListener listener;
    private ApiManager apiManager;
    private boolean isLoading = false;

    public interface OnFragmentInteractionListener {
        void onSellTicketClick();
        void onTicketClick(Ticket ticket);
    }

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiManager = ApiManager.getInstance();
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
        loadTickets();
    }

    // View binding
    private void initViews(View view) {
        rvTickets = view.findViewById(R.id.rv_tickets);
        etSearch = view.findViewById(R.id.et_search);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyTitle = layoutEmptyState.findViewById(R.id.tv_empty_title);
        tvEmptySubtitle = layoutEmptyState.findViewById(R.id.tv_empty_subtitle);
        ivEmptyIcon = layoutEmptyState.findViewById(R.id.iv_empty_icon);
        fabSellTicket = view.findViewById(R.id.fab_sell_ticket);
        skeletonContainer = view.findViewById(R.id.skeleton_container);
    }

    // RecyclerView
    private void setupRecyclerView() {
        tickets = new ArrayList<>();
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        ticketAdapter = new TicketAdapter(tickets, this);
        rvTickets.setAdapter(ticketAdapter);
    }

    // Search
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

    // FAB
    private void setupFab() {
        fabSellTicket.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SellTicketActivity.class));
        });
    }

    // API fetch
    private void loadTickets() {
        // Prevent duplicate loading
        if (isLoading) return;

        isLoading = true;
        setLoading(true);

        apiManager.fetchAvailableTickets(new ApiManager.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> result) {
                isLoading = false;
                if (!isAdded()) return;
                setLoading(false);
                tickets = result;
                ticketAdapter.updateTickets(tickets);
                updateEmptyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                isLoading = false;
                if (!isAdded()) return;
                setLoading(false);
                tickets = new ArrayList<>();
                ticketAdapter.updateTickets(tickets);
                updateEmptyState();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop shimmer animations when fragment is not visible
        if (skeletonContainer != null) {
            skeletonContainer.setVisibility(View.GONE);
        }
    }

    // Empty state
    private void updateEmptyState() {
        if (ticketAdapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTickets.setVisibility(View.GONE);
            ivEmptyIcon.setImageResource(R.drawable.ic_ticket);
            tvEmptyTitle.setText("No tickets available");
            tvEmptySubtitle.setText("Check back later or list your own ticket to get started");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTickets.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            skeletonContainer.setVisibility(View.VISIBLE);
            rvTickets.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            skeletonContainer.setVisibility(View.GONE);
        }
    }

    // Ticket click
    @Override
    public void onTicketClick(Ticket ticket) {
        if (listener != null) listener.onTicketClick(ticket);
    }

    // Called by MainActivity when returning from detail screen
    public void refreshData() {
        loadTickets();
    }
}
