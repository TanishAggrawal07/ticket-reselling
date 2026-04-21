package com.tanish.retix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WalletFragment extends Fragment implements WalletTransactionAdapter.OnTransactionClickListener {

    private RecyclerView rvPurchases, rvSales;
    private View layoutEmptyPurchases, layoutEmptySales;
    private TextView tvEmptyTitlePurchases, tvEmptySubtitlePurchases, tvEmptyIconPurchases;
    private TextView tvEmptyTitleSales, tvEmptySubtitleSales, tvEmptyIconSales;
    private WalletTransactionAdapter purchaseAdapter, saleAdapter;

    private List<WalletTransaction> purchases;
    private List<WalletTransaction> sales;

    public WalletFragment() {
        // Required empty public constructor
    }

    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerViews();
        loadDummyData();
    }

    private void initViews(View view) {
        rvPurchases = view.findViewById(R.id.rv_purchases);
        rvSales = view.findViewById(R.id.rv_sales);
        layoutEmptyPurchases = view.findViewById(R.id.layout_empty_purchases);
        layoutEmptySales = view.findViewById(R.id.layout_empty_sales);
        // Find empty state child views scoped to their respective parent includes
        tvEmptyIconPurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_icon);
        tvEmptyTitlePurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_title);
        tvEmptySubtitlePurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_subtitle);
        tvEmptyIconSales = layoutEmptySales.findViewById(R.id.tv_empty_icon);
        tvEmptyTitleSales = layoutEmptySales.findViewById(R.id.tv_empty_title);
        tvEmptySubtitleSales = layoutEmptySales.findViewById(R.id.tv_empty_subtitle);
    }

    private void setupRecyclerViews() {
        purchases = new ArrayList<>();
        sales = new ArrayList<>();

        rvPurchases.setLayoutManager(new LinearLayoutManager(getContext()));
        purchaseAdapter = new WalletTransactionAdapter(purchases, this);
        rvPurchases.setAdapter(purchaseAdapter);

        rvSales.setLayoutManager(new LinearLayoutManager(getContext()));
        saleAdapter = new WalletTransactionAdapter(sales, this);
        rvSales.setAdapter(saleAdapter);
    }

    private void loadDummyData() {
        // Dummy purchases - using new status system (0 = Processing, 1 = Available)
        purchases.add(new WalletTransaction(
                "Coldplay: Music of the Spheres",
                "Sat, Jan 18 • 7:00 PM",
                5500,
                WalletTransaction.STATUS_PROCESSING,
                true
        ));

        purchases.add(new WalletTransaction(
                "Arijit Singh Live Concert",
                "Fri, Jan 24 • 8:00 PM",
                2800,
                WalletTransaction.STATUS_AVAILABLE,
                true
        ));

        // Dummy sales
        sales.add(new WalletTransaction(
                "IPL 2025: RCB vs CSK",
                "Sat, Mar 15 • 3:30 PM",
                2500,
                WalletTransaction.STATUS_PROCESSING,
                false
        ));

        sales.add(new WalletTransaction(
                "Sunburn Goa 2025",
                "Dec 27-29 • 4:00 PM",
                3200,
                WalletTransaction.STATUS_AVAILABLE,
                false
        ));

        purchaseAdapter.updateTransactions(purchases);
        saleAdapter.updateTransactions(sales);

        updateEmptyStates();
    }

    private void updateEmptyStates() {
        if (purchases.isEmpty()) {
            layoutEmptyPurchases.setVisibility(View.VISIBLE);
            rvPurchases.setVisibility(View.GONE);
            tvEmptyIconPurchases.setText("💳");
            tvEmptyTitlePurchases.setText("No purchases yet");
            tvEmptySubtitlePurchases.setText("Tickets you buy will appear here");
        } else {
            layoutEmptyPurchases.setVisibility(View.GONE);
            rvPurchases.setVisibility(View.VISIBLE);
        }

        if (sales.isEmpty()) {
            layoutEmptySales.setVisibility(View.VISIBLE);
            rvSales.setVisibility(View.GONE);
            tvEmptyIconSales.setText("📦");
            tvEmptyTitleSales.setText("No sales yet");
            tvEmptySubtitleSales.setText("Tickets you sell will appear here");
        } else {
            layoutEmptySales.setVisibility(View.GONE);
            rvSales.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTransactionClick(WalletTransaction transaction) {
        // Could navigate to ticket detail or show more info
    }

    public void addPurchase(WalletTransaction transaction) {
        purchases.add(0, transaction);
        purchaseAdapter.updateTransactions(purchases);
        updateEmptyStates();
    }

    public void addSale(WalletTransaction transaction) {
        sales.add(0, transaction);
        saleAdapter.updateTransactions(sales);
        updateEmptyStates();
    }

    public void updateTransactionStatus(String eventName, int status) {
        for (WalletTransaction t : purchases) {
            if (t.getEventName().equals(eventName)) {
                t.setStatus(status);
                purchaseAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    public WalletTransaction findPurchaseByEvent(String eventName) {
        for (WalletTransaction t : purchases) {
            if (t.getEventName().equals(eventName)) {
                return t;
            }
        }
        return null;
    }
}
