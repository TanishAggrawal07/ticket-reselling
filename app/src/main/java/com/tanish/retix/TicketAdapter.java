package com.tanish.retix;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> tickets;
    private List<Ticket> ticketsFull;
    private OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    public TicketAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets     = new ArrayList<>(tickets);
        this.ticketsFull = new ArrayList<>(tickets);
        this.listener    = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(tickets.get(position));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    // ── Search filter ─────────────────────────────────────────────────────────

    public void filter(String query) {
        List<Ticket> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(ticketsFull);
        } else {
            String lower = query.toLowerCase();
            for (Ticket t : ticketsFull) {
                if (t.getEventName().toLowerCase().contains(lower)) {
                    filtered.add(t);
                }
            }
        }
        tickets = filtered;
        notifyDataSetChanged();
    }

    public void updateTickets(List<Ticket> newTickets) {
        this.tickets     = new ArrayList<>(newTickets);
        this.ticketsFull = new ArrayList<>(newTickets);
        notifyDataSetChanged();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    class TicketViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivEventImage;
        private final TextView  tvNoImageLabel;
        private final TextView  tvEventName, tvDate, tvSellerName, tvRating;
        private final TextView  tvSellingPrice, tvOriginalPrice, tvPricingBadge, tvSellerInitial;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage    = itemView.findViewById(R.id.iv_event_image);
            tvNoImageLabel  = itemView.findViewById(R.id.tv_no_image_label);
            tvEventName     = itemView.findViewById(R.id.tv_event_name);
            tvDate          = itemView.findViewById(R.id.tv_date);
            tvSellerName    = itemView.findViewById(R.id.tv_seller_name);
            tvRating        = itemView.findViewById(R.id.tv_rating);
            tvSellingPrice  = itemView.findViewById(R.id.tv_selling_price);
            tvOriginalPrice = itemView.findViewById(R.id.tv_original_price);
            tvPricingBadge  = itemView.findViewById(R.id.tv_pricing_badge);
            tvSellerInitial = itemView.findViewById(R.id.tv_seller_initial);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTicketClick(tickets.get(pos));
                }
            });
        }

        void bind(Ticket ticket) {
            // ── Text fields ───────────────────────────────────────────────────
            tvEventName.setText(ticket.getEventName());
            tvDate.setText(ticket.getDate());
            tvSellerName.setText(ticket.getSellerName());
            tvRating.setText(String.format("%.1f", ticket.getRating()));
            tvSellingPrice.setText("₹" + ticket.getSellingPrice());
            tvOriginalPrice.setText("₹" + ticket.getOriginalPrice());

            if (ticket.getSellerName() != null && !ticket.getSellerName().isEmpty()) {
                tvSellerInitial.setText(
                        String.valueOf(ticket.getSellerName().charAt(0)).toUpperCase());
            }

            // ── Event image with guaranteed fallback ──────────────────────────
            boolean hasCustomImage = false;

            if (ticket.getEventImageUri() != null && !ticket.getEventImageUri().isEmpty()) {
                // User-uploaded image (URI string)
                try {
                    ivEventImage.setImageURI(Uri.parse(ticket.getEventImageUri()));
                    hasCustomImage = true;
                } catch (Exception e) {
                    // URI no longer accessible — fall through to smart default
                }
            }

            if (!hasCustomImage) {
                // Use smart keyword-based image selection — never blank
                ivEventImage.setImageResource(ticket.getSmartImageResId());
            }

            // Show the subtle "No event image" label only when using a default
            tvNoImageLabel.setVisibility(hasCustomImage ? View.GONE : View.VISIBLE);

            // ── Smart pricing badge ───────────────────────────────────────────
            if (ticket.isDiscounted()) {
                tvPricingBadge.setText("🟢 You save ₹" + ticket.getSavings());
                tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge);
                tvPricingBadge.setTextColor(
                        itemView.getContext().getColor(R.color.success_green));
                tvOriginalPrice.setPaintFlags(
                        tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

            } else if (ticket.getSellingPrice() > ticket.getOriginalPrice()) {
                tvPricingBadge.setText("🔥 Limited availability – secure your spot now");
                tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge_urgent);
                tvPricingBadge.setTextColor(
                        itemView.getContext().getColor(android.R.color.holo_orange_dark));
                tvOriginalPrice.setPaintFlags(
                        tvOriginalPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

            } else {
                tvPricingBadge.setText("Same as original price");
                tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge);
                tvPricingBadge.setTextColor(
                        itemView.getContext().getColor(R.color.text_secondary));
                tvOriginalPrice.setPaintFlags(
                        tvOriginalPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
