package com.tanish.retix;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * WalletFragment — shows the user's wallet balance (real-time) and
 * transaction history (purchases + sales) fetched from Firebase.
 *
 * Database paths used:
 *   users/{uid}/walletBalance          — live balance
 *   transactions/{uid}/{txnId}         — transaction history
 */
public class WalletFragment extends Fragment
        implements WalletTransactionAdapter.OnTransactionClickListener {

    private static final String TAG = "WALLET";

    // ── Balance views ─────────────────────────────────────────────────────────
    private TextView tvBalance;          // main balance display

    // ── Purchases section ─────────────────────────────────────────────────────
    private RecyclerView rvPurchases;
    private View         layoutEmptyPurchases;
    private TextView     tvEmptyIconPurchases, tvEmptyTitlePurchases, tvEmptySubtitlePurchases;
    private WalletTransactionAdapter purchaseAdapter;
    private List<WalletTransaction>  purchases;

    // ── Sales section ─────────────────────────────────────────────────────────
    private RecyclerView rvSales;
    private View         layoutEmptySales;
    private TextView     tvEmptyIconSales, tvEmptyTitleSales, tvEmptySubtitleSales;
    private WalletTransactionAdapter saleAdapter;
    private List<WalletTransaction>  sales;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseManager    firebaseManager;
    private String             currentUid;
    private ValueEventListener balanceListener; // real-time balance listener

    // ── Constructor ───────────────────────────────────────────────────────────

    public WalletFragment() {}

    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();

        // Resolve current user — safe null check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = (user != null) ? user.getUid() : null;

        initViews(view);
        setupRecyclerViews();

        if (currentUid != null) {
            attachBalanceListener();
            loadTransactions();
        } else {
            // Not logged in — show zeros and empty states
            showDefaultBalance();
            updateEmptyStates();
            Log.w(TAG, "No authenticated user — showing empty wallet");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove real-time listener to prevent memory leaks
        if (balanceListener != null && currentUid != null) {
            firebaseManager.removeBalanceListener(currentUid, balanceListener);
            balanceListener = null;
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews(View view) {
        // Balance — optional view; guarded with null check
        tvBalance = view.findViewById(R.id.tv_wallet_balance);

        // Purchases
        rvPurchases          = view.findViewById(R.id.rv_purchases);
        layoutEmptyPurchases = view.findViewById(R.id.layout_empty_purchases);
        if (layoutEmptyPurchases != null) {
            tvEmptyIconPurchases     = layoutEmptyPurchases.findViewById(R.id.tv_empty_icon);
            tvEmptyTitlePurchases    = layoutEmptyPurchases.findViewById(R.id.tv_empty_title);
            tvEmptySubtitlePurchases = layoutEmptyPurchases.findViewById(R.id.tv_empty_subtitle);
        }

        // Sales
        rvSales          = view.findViewById(R.id.rv_sales);
        layoutEmptySales = view.findViewById(R.id.layout_empty_sales);
        if (layoutEmptySales != null) {
            tvEmptyIconSales     = layoutEmptySales.findViewById(R.id.tv_empty_icon);
            tvEmptyTitleSales    = layoutEmptySales.findViewById(R.id.tv_empty_title);
            tvEmptySubtitleSales = layoutEmptySales.findViewById(R.id.tv_empty_subtitle);
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        purchases = new ArrayList<>();
        sales     = new ArrayList<>();

        rvPurchases.setLayoutManager(new LinearLayoutManager(getContext()));
        purchaseAdapter = new WalletTransactionAdapter(purchases, this);
        rvPurchases.setAdapter(purchaseAdapter);

        rvSales.setLayoutManager(new LinearLayoutManager(getContext()));
        saleAdapter = new WalletTransactionAdapter(sales, this);
        rvSales.setAdapter(saleAdapter);
    }

    // ── Balance (real-time) ───────────────────────────────────────────────────

    /**
     * Attaches a ValueEventListener so the balance updates in real time
     * whenever addMoney() or deductMoney() is called anywhere in the app.
     */
    private void attachBalanceListener() {
        balanceListener = firebaseManager.listenToWalletBalance(currentUid,
                new FirebaseManager.BalanceCallback() {
                    @Override
                    public void onSuccess(long balance) {
                        if (!isAdded() || getView() == null) return;
                        Log.d(TAG, "Balance updated: ₹" + balance);
                        displayBalance(balance);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Balance listener error: " + errorMessage);
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
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Fetches all transactions for the current user and splits them into
     * purchases (debits) and sales (credits) for the two RecyclerViews.
     */
    private void loadTransactions() {
        firebaseManager.fetchTransactions(currentUid,
                new FirebaseManager.TransactionsCallback() {
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
                        // Show empty states — don't crash
                        updateEmptyStates();
                    }
                });
    }

    // ── Empty states ──────────────────────────────────────────────────────────

    private void updateEmptyStates() {
        // Purchases
        if (layoutEmptyPurchases != null) {
            if (purchases.isEmpty()) {
                layoutEmptyPurchases.setVisibility(View.VISIBLE);
                rvPurchases.setVisibility(View.GONE);
                if (tvEmptyIconPurchases     != null) tvEmptyIconPurchases.setText("💳");
                if (tvEmptyTitlePurchases    != null) tvEmptyTitlePurchases.setText("No purchases yet");
                if (tvEmptySubtitlePurchases != null) tvEmptySubtitlePurchases.setText("Tickets you buy will appear here");
            } else {
                layoutEmptyPurchases.setVisibility(View.GONE);
                rvPurchases.setVisibility(View.VISIBLE);
            }
        }

        // Sales
        if (layoutEmptySales != null) {
            if (sales.isEmpty()) {
                layoutEmptySales.setVisibility(View.VISIBLE);
                rvSales.setVisibility(View.GONE);
                if (tvEmptyIconSales     != null) tvEmptyIconSales.setText("📦");
                if (tvEmptyTitleSales    != null) tvEmptyTitleSales.setText("No sales yet");
                if (tvEmptySubtitleSales != null) tvEmptySubtitleSales.setText("Tickets you sell will appear here");
            } else {
                layoutEmptySales.setVisibility(View.GONE);
                rvSales.setVisibility(View.VISIBLE);
            }
        }
    }

    // ── Transaction click ─────────────────────────────────────────────────────

    @Override
    public void onTransactionClick(WalletTransaction transaction) {
        // Reserved for future detail view
    }

    // ── Public helpers (called from other fragments/activities) ───────────────

    /** Refreshes both balance and transaction list. */
    public void refresh() {
        if (currentUid == null) return;
        loadTransactions();
        // Balance is already live via the listener — no manual refresh needed
    }

    /**
     * Programmatically adds a purchase entry.
     * Used when a ticket buy flow completes in TicketDetailActivity.
     */
    public void addPurchase(WalletTransaction transaction) {
        if (transaction == null) return;
        purchases.add(0, transaction);
        purchaseAdapter.updateTransactions(purchases);
        updateEmptyStates();
    }

    /**
     * Programmatically adds a sale entry.
     * Used when a ticket listing completes in SellFragment.
     */
    public void addSale(WalletTransaction transaction) {
        if (transaction == null) return;
        sales.add(0, transaction);
        saleAdapter.updateTransactions(sales);
        updateEmptyStates();
    }
}
