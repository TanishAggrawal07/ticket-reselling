package com.tanish.retix;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Legacy wrapper — all logic has been migrated to FirebaseManager (Realtime Database).
 * This class now delegates to FirebaseManager so existing call-sites continue to compile.
 */
public class FirebaseRepository {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static FirebaseRepository instance;

    private FirebaseRepository() {}

    public static FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ── Callback interfaces (kept for compatibility) ───────────────────────────

    public interface TicketsCallback {
        void onSuccess(List<Ticket> tickets);
        void onFailure(Exception e);
    }

    public interface VoidCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ── Delegating methods ────────────────────────────────────────────────────

    public void uploadFile(Uri fileUri, String folder, UploadCallback callback) {
        FirebaseManager.getInstance().uploadFile(fileUri, folder,
                new FirebaseManager.UploadCallback() {
                    @Override public void onSuccess(String downloadUrl) {
                        callback.onSuccess(downloadUrl);
                    }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }

    public void saveTicket(Ticket ticket, VoidCallback callback) {
        FirebaseManager.getInstance().saveTicket(
                ticket.getEventName(),
                ticket.getEventName(),   // description = title
                ticket.getSellingPrice(),
                ticket.getDate(),
                ticket.getEventImageUrl() != null ? ticket.getEventImageUrl() : "",
                ticket.getSellerId(),
                ticket.getSellerName(),
                new FirebaseManager.VoidCallback() {
                    @Override public void onSuccess() { callback.onSuccess(); }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }

    public void fetchAvailableTickets(TicketsCallback callback) {
        FirebaseManager.getInstance().fetchAvailableTickets(
                new FirebaseManager.TicketsCallback() {
                    @Override public void onSuccess(List<Ticket> tickets) {
                        callback.onSuccess(tickets);
                    }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }

    public void fetchMyListings(String sellerId, TicketsCallback callback) {
        FirebaseManager.getInstance().fetchMyListings(sellerId,
                new FirebaseManager.TicketsCallback() {
                    @Override public void onSuccess(List<Ticket> tickets) {
                        callback.onSuccess(tickets);
                    }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }

    public void fetchMyPurchases(String buyerId, TicketsCallback callback) {
        FirebaseManager.getInstance().fetchMyPurchases(buyerId,
                new FirebaseManager.TicketsCallback() {
                    @Override public void onSuccess(List<Ticket> tickets) {
                        callback.onSuccess(tickets);
                    }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }

    public void markTicketSold(String ticketId, String buyerId, VoidCallback callback) {
        FirebaseManager.getInstance().markTicketSold(ticketId, buyerId,
                new FirebaseManager.VoidCallback() {
                    @Override public void onSuccess() { callback.onSuccess(); }
                    @Override public void onFailure(String errorMessage) {
                        callback.onFailure(new Exception(errorMessage));
                    }
                });
    }
}
