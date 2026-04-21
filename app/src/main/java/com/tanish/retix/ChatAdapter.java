package com.tanish.retix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_BUYER  = 1;
    private static final int VIEW_TYPE_SELLER = 2;

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isBuyer() ? VIEW_TYPE_BUYER : VIEW_TYPE_SELLER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.bind(msg);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutBuyer, layoutSeller;
        private TextView tvBuyerText, tvBuyerTime;
        private TextView tvSellerText, tvSellerTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBuyer  = itemView.findViewById(R.id.layout_buyer_message);
            layoutSeller = itemView.findViewById(R.id.layout_seller_message);
            tvBuyerText  = itemView.findViewById(R.id.tv_buyer_text);
            tvBuyerTime  = itemView.findViewById(R.id.tv_buyer_time);
            tvSellerText = itemView.findViewById(R.id.tv_seller_text);
            tvSellerTime = itemView.findViewById(R.id.tv_seller_time);
        }

        public void bind(ChatMessage msg) {
            if (msg.isBuyer()) {
                layoutBuyer.setVisibility(View.VISIBLE);
                layoutSeller.setVisibility(View.GONE);
                tvBuyerText.setText(msg.getText());
                tvBuyerTime.setText(msg.getTime());
            } else {
                layoutBuyer.setVisibility(View.GONE);
                layoutSeller.setVisibility(View.VISIBLE);
                tvSellerText.setText(msg.getText());
                tvSellerTime.setText(msg.getTime());
            }
        }
    }
}
