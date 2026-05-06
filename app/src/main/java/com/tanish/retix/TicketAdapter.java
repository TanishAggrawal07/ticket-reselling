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
        private final TextView  tvSellingPrice, tvOriginalPrice, tvPricingBadge, tvSellerInitial, tvSoldBadge;

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
            tvSoldBadge = itemView.findViewById(R.id.tv_sold_badge);

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
            if (ticket.getRating() > 0) {
                tvRating.setText(String.format("%.1f", ticket.getRating()));
                tvRating.setVisibility(View.VISIBLE);
            } else {
                tvRating.setVisibility(View.GONE);
            }
            tvSellingPrice.setText("₹" + ticket.getSellingPrice());
            tvOriginalPrice.setText("₹" + ticket.getOriginalPrice());

            if (ticket.getSellerName() != null && !ticket.getSellerName().isEmpty()) {
                tvSellerInitial.setText(
                        String.valueOf(ticket.getSellerName().charAt(0)).toUpperCase());
            }

            // ── Event image ───────────────────────────────────────────────────
            loadEventImage(ticket);

            // ── Smart pricing badge ───────────────────────────────────────────
            if (ticket.isDiscounted()) {
                tvPricingBadge.setText("You save ₹" + ticket.getSavings());
                tvPricingBadge.setBackgroundResource(R.drawable.bg_pricing_badge);
                tvPricingBadge.setTextColor(
                        itemView.getContext().getColor(R.color.success_green));
                tvOriginalPrice.setPaintFlags(
                        tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (ticket.getSellingPrice() > ticket.getOriginalPrice()) {
                tvPricingBadge.setText("Limited availability");
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

            // ── Sold badge ─────────────────────────────────────────────────────
            if (tvSoldBadge != null) {
                if (Ticket.STATUS_SOLD.equals(ticket.getStatus())) {
                    tvSoldBadge.setVisibility(View.VISIBLE);
                } else {
                    tvSoldBadge.setVisibility(View.GONE);
                }
            }
        }

        /**
         * Image loading priority:
         *   1. Cloudinary / HTTPS URL (from Firebase DB imageUrl field)
         *   2. Local content URI (pre-upload preview)
         *   3. Smart keyword-based drawable fallback
         */
        private void loadEventImage(Ticket ticket) {
            String imageUrl = ticket.getEventImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                loadFromHttpUrl(imageUrl, ticket);
                tvNoImageLabel.setVisibility(View.GONE);
                return;
            }

            String localUri = ticket.getEventImageUri();
            if (localUri != null && !localUri.isEmpty()) {
                try {
                    ivEventImage.setImageURI(Uri.parse(localUri));
                    tvNoImageLabel.setVisibility(View.GONE);
                    return;
                } catch (Exception ignored) {}
            }

            // Smart drawable fallback — never blank
            ivEventImage.setImageResource(ticket.getSmartImageResId());
            tvNoImageLabel.setVisibility(View.VISIBLE);
        }

        /**
         * Loads an image from any HTTPS URL (Cloudinary or Firebase Storage download URL)
         * on a background thread. Falls back to the smart drawable on failure.
         */
        private void loadFromHttpUrl(String url, Ticket ticket) {
            new Thread(() -> {
                try {
                    java.net.URL imageUrl = new java.net.URL(url);
                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection) imageUrl.openConnection();
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(15_000);
                    conn.setDoInput(true);
                    conn.connect();
                    java.io.InputStream input = conn.getInputStream();
                    android.graphics.Bitmap bmp =
                            android.graphics.BitmapFactory.decodeStream(input);
                    input.close();

                    // Post result back to the main thread
                    if (bmp != null) {
                        android.os.Handler mainHandler =
                                new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> ivEventImage.setImageBitmap(bmp));
                    } else {
                        android.os.Handler mainHandler =
                                new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() ->
                                ivEventImage.setImageResource(ticket.getSmartImageResId()));
                    }
                } catch (Exception e) {
                    android.os.Handler mainHandler =
                            new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() ->
                            ivEventImage.setImageResource(ticket.getSmartImageResId()));
                }
            }).start();
        }
    }
}
