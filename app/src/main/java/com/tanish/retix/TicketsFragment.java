package com.tanish.retix;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * TicketsFragment - Displays all tickets relevant to the user:
 * both purchased tickets and their own listings.
 */
public class TicketsFragment extends Fragment implements TicketAdapter.OnTicketClickListener {

    private RecyclerView rvMyTickets;
    private TicketAdapter ticketAdapter;
    private View layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private ImageView ivEmptyIcon;
    private View skeletonContainer;

    private List<Ticket> tickets;
    private ApiManager apiManager;
    private boolean isLoading = false;

    public TicketsFragment() {}

    public static TicketsFragment newInstance() {
        return new TicketsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiManager = ApiManager.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadAllTickets();
    }

    private void initViews(View view) {
        rvMyTickets = view.findViewById(R.id.rv_my_tickets);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyTitle = layoutEmptyState.findViewById(R.id.tv_empty_title);
        tvEmptySubtitle = layoutEmptyState.findViewById(R.id.tv_empty_subtitle);
        ivEmptyIcon = layoutEmptyState.findViewById(R.id.iv_empty_icon);
        skeletonContainer = view.findViewById(R.id.skeleton_container);
    }

    private void setupRecyclerView() {
        tickets = new ArrayList<>();
        rvMyTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        ticketAdapter = new TicketAdapter(tickets, this);
        rvMyTickets.setAdapter(ticketAdapter);
    }

    private void loadAllTickets() {
        if (isLoading) return;
        isLoading = true;
        setLoading(true);

        String userId = ApiClient.getTokenManager().getUserId();
        if (userId == null) {
            isLoading = false;
            setLoading(false);
            tickets = new ArrayList<>();
            ticketAdapter.updateTickets(tickets);
            updateEmptyState();
            return;
        }

        // Load both purchases and listings in parallel
        List<Ticket> combined = new ArrayList<>();
        int[] pending = {2};

        apiManager.fetchMyPurchases(userId, new ApiManager.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> result) {
                synchronized (combined) {
                    combined.addAll(result);
                }
                if (--pending[0] == 0) {
                    isLoading = false;
                    setLoading(false);
                    tickets = combined;
                    ticketAdapter.updateTickets(tickets);
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (--pending[0] == 0) {
                    isLoading = false;
                    setLoading(false);
                    tickets = combined;
                    ticketAdapter.updateTickets(tickets);
                    updateEmptyState();
                }
            }
        });

        apiManager.fetchMyListings(userId, new ApiManager.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> result) {
                synchronized (combined) {
                    combined.addAll(result);
                }
                if (--pending[0] == 0) {
                    isLoading = false;
                    setLoading(false);
                    tickets = combined;
                    ticketAdapter.updateTickets(tickets);
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (--pending[0] == 0) {
                    isLoading = false;
                    setLoading(false);
                    tickets = combined;
                    ticketAdapter.updateTickets(tickets);
                    updateEmptyState();
                }
            }
        });
    }

    private void updateEmptyState() {
        if (ticketAdapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvMyTickets.setVisibility(View.GONE);
            ivEmptyIcon.setImageResource(R.drawable.ic_ticket);
            tvEmptyTitle.setText("No tickets yet");
            tvEmptySubtitle.setText("Tickets you buy or list will appear here");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvMyTickets.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            skeletonContainer.setVisibility(View.VISIBLE);
            rvMyTickets.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            skeletonContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
        intent.putExtra("ticket", ticket);
        String userId = ApiClient.getTokenManager().getUserId();

        // Determine if user is seller or buyer
        boolean isOwnTicket = userId != null && userId.equals(ticket.getSellerId());
        boolean isSold = Ticket.STATUS_SOLD.equals(ticket.getStatus());

        // Pass extra info for seller chat
        if (isOwnTicket) {
            intent.putExtra("is_purchased", false);
        } else {
            intent.putExtra("is_purchased", true);
        }

        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllTickets();
    }

    public void refreshData() {
        loadAllTickets();
    }
}