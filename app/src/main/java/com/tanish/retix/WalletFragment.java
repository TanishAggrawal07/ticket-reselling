package com.tanish.retix;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * WalletFragment - Shows wallet balance and transaction history using the API.
 */
public class WalletFragment extends Fragment
        implements WalletTransactionAdapter.OnTransactionClickListener {

    private static final String TAG = "WALLET";

    // Balance views
    private TextView tvBalance;
    private TextView tvPendingBalance;
    private View layoutPendingBalance;

    // Purchases section
    private RecyclerView rvPurchases;
    private View layoutEmptyPurchases;
    private ImageView ivEmptyIconPurchases;
    private TextView tvEmptyTitlePurchases, tvEmptySubtitlePurchases;
    private WalletTransactionAdapter purchaseAdapter;
    private List<WalletTransaction> purchases;

    // Sales section
    private RecyclerView rvSales;
    private View layoutEmptySales;
    private ImageView ivEmptyIconSales;
    private TextView tvEmptyTitleSales, tvEmptySubtitleSales;
    private WalletTransactionAdapter saleAdapter;
    private List<WalletTransaction> sales;

    // API
    private ApiManager apiManager;
    private String currentUid;

    // Polling for balance updates
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final long POLL_INTERVAL_MS = 5000; // Poll every 5 seconds

    public WalletFragment() {}

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

        apiManager = ApiManager.getInstance();
        currentUid = ApiClient.getTokenManager().getUserId();

        initViews(view);
        setupRecyclerViews();

        if (currentUid != null) {
            loadBalance();
            loadTransactions();
            startPolling();
        } else {
            showDefaultBalance();
            updateEmptyStates();
            Log.w(TAG, "No authenticated user — showing empty wallet");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUid != null && pollRunnable == null) {
            startPolling();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    // View binding
    private void initViews(View view) {
        tvBalance = view.findViewById(R.id.tv_wallet_balance);
        tvPendingBalance = view.findViewById(R.id.tv_pending_balance);
        layoutPendingBalance = view.findViewById(R.id.layout_pending_balance);

        rvPurchases = view.findViewById(R.id.rv_purchases);
        layoutEmptyPurchases = view.findViewById(R.id.layout_empty_purchases);
        if (layoutEmptyPurchases != null) {
            ivEmptyIconPurchases = layoutEmptyPurchases.findViewById(R.id.iv_empty_icon);
            tvEmptyTitlePurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_title);
            tvEmptySubtitlePurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_subtitle);
        }

        rvSales = view.findViewById(R.id.rv_sales);
        layoutEmptySales = view.findViewById(R.id.layout_empty_sales);
        if (layoutEmptySales != null) {
            ivEmptyIconSales = layoutEmptySales.findViewById(R.id.iv_empty_icon);
            tvEmptyTitleSales = layoutEmptySales.findViewById(R.id.tv_empty_title);
            tvEmptySubtitleSales = layoutEmptySales.findViewById(R.id.tv_empty_subtitle);
        }
    }

    // RecyclerViews
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

    // Polling for balance
    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                loadBalance();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    // Balance
    private void loadBalance() {
        apiManager.getWalletBalance(currentUid, new ApiManager.BalanceCallback() {
            @Override
            public void onSuccess(long balance, long pendingBalance) {
                if (!isAdded()) return;
                displayBalance(balance);
                displayPendingBalance(pendingBalance);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                showDefaultBalance();
            }
        });
    }

    private void displayBalance(long balance) {
        if (tvBalance != null) {
            tvBalance.setText("₹" + balance);
        }
    }

    private void showDefaultBalance() {
        if (tvBalance != null) {
            tvBalance.setText("₹0");
        }
        if (layoutPendingBalance != null) {
            layoutPendingBalance.setVisibility(View.GONE);
        }
    }

    private void displayPendingBalance(long pendingBalance) {
        if (layoutPendingBalance != null && tvPendingBalance != null) {
            if (pendingBalance > 0) {
                tvPendingBalance.setText("₹" + pendingBalance + " processing");
                layoutPendingBalance.setVisibility(View.VISIBLE);
            } else {
                layoutPendingBalance.setVisibility(View.GONE);
            }
        }
    }

    // Transactions
    private void loadTransactions() {
        apiManager.fetchTransactions(currentUid, new ApiManager.TransactionsCallback() {
            @Override
            public void onSuccess(List<WalletTransaction> transactions) {
                if (!isAdded() || getView() == null) return;
                Log.d(TAG, "Transactions loaded: " + transactions.size());

                purchases.clear();
                sales.clear();

                for (WalletTransaction txn : transactions) {
                    if (txn == null) continue;
                    if (txn.isPurchase()) {
                        purchases.add(txn);
                    } else {
                        sales.add(txn);
                    }
                }

                purchaseAdapter.updateTransactions(purchases);
                saleAdapter.updateTransactions(sales);
                updateEmptyStates();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load transactions: " + errorMessage);
                updateEmptyStates();
            }
        });
    }

    // Empty states
    private void updateEmptyStates() {
        if (layoutEmptyPurchases != null) {
            if (purchases.isEmpty()) {
                layoutEmptyPurchases.setVisibility(View.VISIBLE);
                rvPurchases.setVisibility(View.GONE);
                if (ivEmptyIconPurchases != null) ivEmptyIconPurchases.setImageResource(R.drawable.ic_credit_card);
                if (tvEmptyTitlePurchases != null) tvEmptyTitlePurchases.setText("No purchases yet");
                if (tvEmptySubtitlePurchases != null) tvEmptySubtitlePurchases.setText("Tickets you buy will appear here");
            } else {
                layoutEmptyPurchases.setVisibility(View.GONE);
                rvPurchases.setVisibility(View.VISIBLE);
            }
        }

        if (layoutEmptySales != null) {
            if (sales.isEmpty()) {
                layoutEmptySales.setVisibility(View.VISIBLE);
                rvSales.setVisibility(View.GONE);
                if (ivEmptyIconSales != null) ivEmptyIconSales.setImageResource(R.drawable.ic_package);
                if (tvEmptyTitleSales != null) tvEmptyTitleSales.setText("No sales yet");
                if (tvEmptySubtitleSales != null) tvEmptySubtitleSales.setText("Tickets you sell will appear here");
            } else {
                layoutEmptySales.setVisibility(View.GONE);
                rvSales.setVisibility(View.VISIBLE);
            }
        }
    }

    // Transaction click
    @Override
    public void onTransactionClick(WalletTransaction transaction) {
        // Reserved for future detail view
    }

    // Public helpers
    public void refresh() {
        if (currentUid == null) return;
        loadTransactions();
    }

    public void addPurchase(WalletTransaction transaction) {
        if (transaction == null) return;
        purchases.add(0, transaction);
        purchaseAdapter.updateTransactions(purchases);
        updateEmptyStates();
    }

    public void addSale(WalletTransaction transaction) {
        if (transaction == null) return;
        sales.add(0, transaction);
        saleAdapter.updateTransactions(sales);
        updateEmptyStates();
    }
}
