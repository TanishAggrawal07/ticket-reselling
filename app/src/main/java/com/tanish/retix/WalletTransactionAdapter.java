package com.tanish.retix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.TransactionViewHolder> {

    private List<WalletTransaction> transactions;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(WalletTransaction transaction);
    }

    public WalletTransactionAdapter(List<WalletTransaction> transactions, OnTransactionClickListener listener) {
        this.transactions = new ArrayList<>(transactions);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        WalletTransaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<WalletTransaction> newTransactions) {
        this.transactions = new ArrayList<>(newTransactions);
        notifyDataSetChanged();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventName, tvPrice, tvDate, tvStatus;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tv_event_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(transactions.get(position));
                }
            });
        }

        public void bind(WalletTransaction transaction) {
            tvEventName.setText(transaction.getEventName());
            tvPrice.setText("₹" + transaction.getPrice());
            tvDate.setText(transaction.getDate());

            if (transaction.isAvailable()) {
                tvStatus.setText("Available");
                tvStatus.setBackgroundResource(R.drawable.bg_status_available);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.success_green));
            } else {
                tvStatus.setText("Processing");
                tvStatus.setBackgroundResource(R.drawable.bg_status_processing);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.warning_amber));
            }
        }
    }
}
