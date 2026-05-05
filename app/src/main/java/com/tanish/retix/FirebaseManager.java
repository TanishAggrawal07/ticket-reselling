package com.tanish.retix;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FirebaseManager — single helper class for all Firebase Realtime Database,
 * Firebase Auth, and Firebase Storage operations.
 *
 * Database structure:
 *   users/{uid}
 *     uid, name, email, profileImage, createdAt
 *     myTickets/{ticketId}: true
 *
 *   tickets/{ticketId}
 *     ticketId, title, description, price, eventDate,
 *     imageUrl, sellerId, sellerName, createdAt, status
 *
 *   chats/{chatId}/messages/{messageId}
 *     senderId, receiverId, message, timestamp
 */
public class FirebaseManager {

    // ── Database path constants ───────────────────────────────────────────────
    public static final String PATH_USERS      = "users";
    public static final String PATH_TICKETS    = "tickets";
    public static final String PATH_CHATS      = "chats";
    public static final String PATH_MESSAGES   = "messages";
    public static final String PATH_MY_TICKETS = "myTickets";

    // ── Ticket field constants ────────────────────────────────────────────────
    public static final String FIELD_TICKET_ID   = "ticketId";
    public static final String FIELD_TITLE       = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PRICE       = "price";
    public static final String FIELD_EVENT_DATE  = "eventDate";
    public static final String FIELD_IMAGE_URL   = "imageUrl";
    public static final String FIELD_SELLER_ID   = "sellerId";
    public static final String FIELD_SELLER_NAME = "sellerName";
    public static final String FIELD_CREATED_AT  = "createdAt";
    public static final String FIELD_STATUS      = "status";

    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_SOLD      = "sold";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static FirebaseManager instance;

    private final FirebaseDatabase  database;
    private final FirebaseAuth      auth;
    private final FirebaseStorage   storage;

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        // Enable offline persistence so the app works without network
        database.setPersistenceEnabled(true);
        auth    = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) instance = new FirebaseManager();
        return instance;
    }

    // ── Callback interfaces ───────────────────────────────────────────────────

    public interface VoidCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface TicketsCallback {
        void onSuccess(List<Ticket> tickets);
        void onFailure(String errorMessage);
    }

    public interface MessagesCallback {
        void onNewMessage(ChatMessage message);
        void onFailure(String errorMessage);
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    /** Callback for a single user profile read. */
    public interface UserProfileCallback {
        void onSuccess(String name, String email, String profileImageUrl);
        void onFailure(String errorMessage);
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /** Returns the currently signed-in Firebase user, or null if not signed in. */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /** Returns the UID of the current user, or null. */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** Returns the display name of the current user, or "User" as fallback. */
    public String getCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        return "User";
    }

    // ── User system ───────────────────────────────────────────────────────────

    /**
     * Saves or updates a user profile at users/{uid}.
     * Called after signup to create the initial user record.
     *
     * @param uid          Firebase Auth UID
     * @param name         display name
     * @param email        email address
     * @param profileImage profile image URL (pass "" if none)
     * @param callback     result callback
     */
    public void saveUser(String uid, String name, String email,
                         String profileImage, VoidCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid",           uid);
        userMap.put("name",          name != null ? name : "");
        userMap.put("email",         email != null ? email : "");
        userMap.put("profileImage",  profileImage != null ? profileImage : "");
        userMap.put("createdAt",     System.currentTimeMillis());
        // Initialize wallet balance to 0 so getWalletBalance() never reads null
        userMap.put("walletBalance", 0);

        android.util.Log.d("FIREBASE", "saveUser: writing to users/" + uid);

        database.getReference(PATH_USERS)
                .child(uid)
                .setValue(userMap)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("FIREBASE", "saveUser: success for uid=" + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FIREBASE", "saveUser: failed — " + e.getMessage());
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Failed to save user");
                });
    }

    // ── Wallet system ─────────────────────────────────────────────────────────

    /**
     * Database paths for the wallet system:
     *   users/{uid}/walletBalance          — current balance (long, default 0)
     *   transactions/{uid}/{txnId}         — transaction history
     *     txnId, type ("credit"/"debit"), amount, description, timestamp
     */
    public static final String PATH_TRANSACTIONS   = "transactions";
    public static final String WALLET_BALANCE_KEY  = "walletBalance";
    public static final String TXN_TYPE_CREDIT     = "credit";
    public static final String TXN_TYPE_DEBIT      = "debit";

    /** Callback that delivers the current wallet balance. */
    public interface BalanceCallback {
        void onSuccess(long balance);
        void onFailure(String errorMessage);
    }

    /** Callback that delivers a list of WalletTransaction objects. */
    public interface TransactionsCallback {
        void onSuccess(List<WalletTransaction> transactions);
        void onFailure(String errorMessage);
    }

    /**
     * Reads the current wallet balance for a user.
     * Returns 0 safely if the field is missing — never crashes on null.
     *
     * @param uid      Firebase Auth UID
     * @param callback delivers the balance (≥ 0)
     */
    public void getWalletBalance(String uid, BalanceCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onSuccess(0);
            return;
        }

        database.getReference(PATH_USERS)
                .child(uid)
                .child(WALLET_BALANCE_KEY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long balance = 0;
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            Object val = snapshot.getValue();
                            if (val instanceof Long)   balance = (Long) val;
                            if (val instanceof Double) balance = ((Double) val).longValue();
                            if (val instanceof Integer) balance = ((Integer) val).longValue();
                        }
                        callback.onSuccess(balance);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Return 0 on error — never crash the wallet screen
                        callback.onSuccess(0);
                    }
                });
    }

    /**
     * Attaches a real-time listener to the wallet balance.
     * Fires immediately with the current value, then on every change.
     * Returns the ValueEventListener so the caller can remove it in onDestroy().
     *
     * @param uid      Firebase Auth UID
     * @param callback fires with the updated balance
     */
    public ValueEventListener listenToWalletBalance(String uid, BalanceCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onSuccess(0);
            return null;
        }

        DatabaseReference ref = database.getReference(PATH_USERS)
                .child(uid)
                .child(WALLET_BALANCE_KEY);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long balance = 0;
                if (snapshot.exists() && snapshot.getValue() != null) {
                    Object val = snapshot.getValue();
                    if (val instanceof Long)    balance = (Long) val;
                    if (val instanceof Double)  balance = ((Double) val).longValue();
                    if (val instanceof Integer) balance = ((Integer) val).longValue();
                }
                callback.onSuccess(balance);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onSuccess(0);
            }
        };

        ref.addValueEventListener(listener);
        return listener;
    }

    /** Removes a wallet balance listener. */
    public void removeBalanceListener(String uid, ValueEventListener listener) {
        if (uid == null || listener == null) return;
        database.getReference(PATH_USERS)
                .child(uid)
                .child(WALLET_BALANCE_KEY)
                .removeEventListener(listener);
    }

    /**
     * Adds money to a user's wallet and records a credit transaction.
     * Uses a Firebase transaction to prevent race conditions.
     *
     * @param uid         Firebase Auth UID
     * @param amount      amount to add (must be > 0)
     * @param description human-readable reason (e.g. "Ticket sold: Coldplay")
     * @param callback    result callback
     */
    public void addMoney(String uid, long amount, String description, VoidCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        if (amount <= 0) {
            callback.onFailure("Amount must be greater than 0");
            return;
        }

        DatabaseReference balanceRef = database.getReference(PATH_USERS)
                .child(uid)
                .child(WALLET_BALANCE_KEY);

        // runTransaction ensures atomic read-modify-write — no race conditions
        balanceRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(
                    @NonNull com.google.firebase.database.MutableData currentData) {
                Long current = 0L;
                if (currentData.getValue() != null) {
                    Object val = currentData.getValue();
                    if (val instanceof Long)    current = (Long) val;
                    if (val instanceof Double)  current = ((Double) val).longValue();
                    if (val instanceof Integer) current = ((Integer) val).longValue();
                }
                currentData.setValue(current + amount);
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot currentData) {
                if (error != null || !committed) {
                    callback.onFailure(error != null ? error.getMessage() : "Transaction failed");
                    return;
                }
                // Record the credit transaction in history
                recordTransaction(uid, TXN_TYPE_CREDIT, amount,
                        description != null ? description : "Credit", callback);
            }
        });
    }

    /**
     * Deducts money from a user's wallet and records a debit transaction.
     * Fails safely if the balance would go negative.
     *
     * @param uid         Firebase Auth UID
     * @param amount      amount to deduct (must be > 0)
     * @param description human-readable reason (e.g. "Ticket purchased: Coldplay")
     * @param callback    result callback
     */
    public void deductMoney(String uid, long amount, String description, VoidCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        if (amount <= 0) {
            callback.onFailure("Amount must be greater than 0");
            return;
        }

        DatabaseReference balanceRef = database.getReference(PATH_USERS)
                .child(uid)
                .child(WALLET_BALANCE_KEY);

        balanceRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(
                    @NonNull com.google.firebase.database.MutableData currentData) {
                Long current = 0L;
                if (currentData.getValue() != null) {
                    Object val = currentData.getValue();
                    if (val instanceof Long)    current = (Long) val;
                    if (val instanceof Double)  current = ((Double) val).longValue();
                    if (val instanceof Integer) current = ((Integer) val).longValue();
                }
                if (current < amount) {
                    // Abort — insufficient balance
                    return com.google.firebase.database.Transaction.abort();
                }
                currentData.setValue(current - amount);
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot currentData) {
                if (!committed) {
                    callback.onFailure(error != null
                            ? error.getMessage()
                            : "Insufficient balance");
                    return;
                }
                recordTransaction(uid, TXN_TYPE_DEBIT, amount,
                        description != null ? description : "Debit", callback);
            }
        });
    }

    /**
     * Writes a transaction record to transactions/{uid}/{txnId}.
     * Called internally by addMoney() and deductMoney().
     */
    private void recordTransaction(String uid, String type, long amount,
                                   String description, VoidCallback callback) {
        DatabaseReference txnRef = database.getReference(PATH_TRANSACTIONS)
                .child(uid);
        String txnId = txnRef.push().getKey();
        if (txnId == null) {
            // Balance was already updated — don't fail the whole operation
            callback.onSuccess();
            return;
        }

        Map<String, Object> txnMap = new HashMap<>();
        txnMap.put("txnId",       txnId);
        txnMap.put("type",        type);
        txnMap.put("amount",      amount);
        txnMap.put("description", description);
        txnMap.put("timestamp",   System.currentTimeMillis());

        txnRef.child(txnId)
                .setValue(txnMap)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    // Transaction history write failed but balance is already updated.
                    // Log the error but still report success to the caller.
                    android.util.Log.e("WALLET",
                            "Failed to record transaction: " + e.getMessage());
                    callback.onSuccess();
                });
    }

    /**
     * Fetches all transaction history for a user, ordered by timestamp descending.
     *
     * @param uid      Firebase Auth UID
     * @param callback delivers a list of WalletTransaction objects
     */
    public void fetchTransactions(String uid, TransactionsCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        database.getReference(PATH_TRANSACTIONS)
                .child(uid)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<WalletTransaction> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            WalletTransaction txn = snapshotToWalletTransaction(child);
                            if (txn != null) list.add(txn);
                        }
                        // Reverse so newest is first
                        java.util.Collections.reverse(list);
                        callback.onSuccess(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    /**
     * Converts a transactions/{uid}/{txnId} DataSnapshot into a WalletTransaction.
     * Returns null on any error — never crashes.
     */
    private WalletTransaction snapshotToWalletTransaction(DataSnapshot snapshot) {
        try {
            if (snapshot == null || !snapshot.exists()) return null;

            String type        = safeString(snapshot, "type");
            String description = safeString(snapshot, "description");
            long   timestamp   = 0;
            long   amount      = 0;

            Object tsObj  = snapshot.child("timestamp").getValue();
            Object amtObj = snapshot.child("amount").getValue();

            if (tsObj  instanceof Long)    timestamp = (Long) tsObj;
            if (tsObj  instanceof Double)  timestamp = ((Double) tsObj).longValue();
            if (amtObj instanceof Long)    amount    = (Long) amtObj;
            if (amtObj instanceof Double)  amount    = ((Double) amtObj).longValue();
            if (amtObj instanceof Integer) amount    = ((Integer) amtObj).longValue();

            // Format timestamp as a readable date string
            String dateStr = new java.text.SimpleDateFormat(
                    "dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp));

            boolean isCredit = TXN_TYPE_CREDIT.equals(type);

            // Map to the existing WalletTransaction model:
            //   isPurchase = false for credits (sales), true for debits (purchases)
            int status = WalletTransaction.STATUS_AVAILABLE;
            return new WalletTransaction(
                    description != null ? description : "",
                    dateStr,
                    (int) amount,
                    status,
                    !isCredit   // debit = purchase, credit = sale
            );
        } catch (Exception e) {
            android.util.Log.e("WALLET", "Failed to parse transaction: " + e.getMessage());
            return null;
        }
    }

    // ── Ticket system ─────────────────────────────────────────────────────────

    /**
     * Fetches a user's profile from users/{uid}.
     * All fields are read safely — never throws on null or missing data.
     */
    public void fetchUserProfile(String uid, UserProfileCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }

        android.util.Log.d("FIREBASE", "fetchUserProfile: reading users/" + uid);

        database.getReference(PATH_USERS)
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        android.util.Log.d("PROFILE", "Data: " + snapshot.getValue());

                        if (!snapshot.exists()) {
                            // Node doesn't exist yet — return empty strings, not null
                            callback.onSuccess("", "", "");
                            return;
                        }

                        String name         = safeString(snapshot, "name");
                        String email        = safeString(snapshot, "email");
                        String profileImage = safeString(snapshot, "profileImage");

                        // Never pass null — use empty string as safe default
                        callback.onSuccess(
                                name         != null ? name         : "",
                                email        != null ? email        : "",
                                profileImage != null ? profileImage : ""
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("PROFILE", "Fetch failed: " + error.getMessage());
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    /**
     * Saves a ticket to tickets/{ticketId} and records it under
     * users/{sellerId}/myTickets/{ticketId}: true.
     *
     * @param title        event title
     * @param description  event description
     * @param price        selling price
     * @param eventDate    event date string
     * @param imageUrl     Cloudinary / Storage image URL (pass "" if none)
     * @param sellerId     Firebase Auth UID of the seller
     * @param sellerName   display name of the seller
     * @param callback     result callback
     */
    public void saveTicket(String title, String description, int price,
                           String eventDate, String imageUrl,
                           String sellerId, String sellerName,
                           VoidCallback callback) {
        if (sellerId == null || sellerId.isEmpty()) {
            callback.onFailure("You must be logged in to list a ticket");
            return;
        }

        // Generate a unique ticket ID using push()
        DatabaseReference ticketsRef = database.getReference(PATH_TICKETS);
        String ticketId = ticketsRef.push().getKey();
        if (ticketId == null) {
            callback.onFailure("Failed to generate ticket ID");
            return;
        }

        Map<String, Object> ticketMap = new HashMap<>();
        ticketMap.put(FIELD_TICKET_ID,   ticketId);
        ticketMap.put(FIELD_TITLE,       title != null ? title : "");
        ticketMap.put(FIELD_DESCRIPTION, description != null ? description : "");
        ticketMap.put(FIELD_PRICE,       price);
        ticketMap.put(FIELD_EVENT_DATE,  eventDate != null ? eventDate : "");
        ticketMap.put(FIELD_IMAGE_URL,   imageUrl != null ? imageUrl : "");
        ticketMap.put(FIELD_SELLER_ID,   sellerId);
        ticketMap.put(FIELD_SELLER_NAME, sellerName != null ? sellerName : "");
        ticketMap.put(FIELD_CREATED_AT,  System.currentTimeMillis());
        ticketMap.put(FIELD_STATUS,      STATUS_AVAILABLE);

        // Use multi-path update to write both nodes atomically
        Map<String, Object> multiUpdate = new HashMap<>();
        multiUpdate.put(PATH_TICKETS + "/" + ticketId, ticketMap);
        multiUpdate.put(PATH_USERS + "/" + sellerId + "/" + PATH_MY_TICKETS + "/" + ticketId, true);

        android.util.Log.d("FIREBASE", "saveTicket: writing ticketId=" + ticketId
                + " sellerId=" + sellerId + " title=" + title + " imageUrl=" + imageUrl);

        database.getReference()
                .updateChildren(multiUpdate)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("FIREBASE", "saveTicket: success — ticketId=" + ticketId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FIREBASE", "saveTicket: failed — " + e.getMessage());
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Failed to save ticket");
                });
    }

    /**
     * Fetches all tickets with status = "available", ordered by createdAt descending
     * (latest first). Uses a one-time read.
     */
    public void fetchAvailableTickets(TicketsCallback callback) {
        Query query = database.getReference(PATH_TICKETS)
                .orderByChild(FIELD_CREATED_AT);

        android.util.Log.d("FIREBASE", "fetchAvailableTickets: starting query");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("FIREBASE", "fetchAvailableTickets: snapshot received, childCount="
                        + snapshot.getChildrenCount());
                List<Ticket> tickets = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Ticket t = snapshotToTicket(child);
                    if (t != null && STATUS_AVAILABLE.equals(t.getStatus())) {
                        tickets.add(t);
                    }
                }
                // Reverse so latest is first (RTDB orders ascending)
                java.util.Collections.reverse(tickets);
                android.util.Log.d("FIREBASE", "fetchAvailableTickets: returning " + tickets.size() + " available tickets");
                callback.onSuccess(tickets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FIREBASE", "fetchAvailableTickets: cancelled — " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        });
    }

    /**
     * Fetches all tickets listed by a specific seller (My Listings).
     */
    public void fetchMyListings(String sellerId, TicketsCallback callback) {
        if (sellerId == null || sellerId.isEmpty()) {
            callback.onFailure("Invalid seller ID");
            return;
        }

        database.getReference(PATH_TICKETS)
                .orderByChild(FIELD_SELLER_ID)
                .equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Ticket> tickets = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Ticket t = snapshotToTicket(child);
                            if (t != null) tickets.add(t);
                        }
                        java.util.Collections.reverse(tickets);
                        callback.onSuccess(tickets);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    /**
     * Fetches all tickets purchased by a specific buyer (My Purchases).
     * Requires a "buyerId" index in Firebase Realtime Database rules.
     */
    public void fetchMyPurchases(String buyerId, TicketsCallback callback) {
        if (buyerId == null || buyerId.isEmpty()) {
            callback.onFailure("Invalid buyer ID");
            return;
        }

        database.getReference(PATH_TICKETS)
                .orderByChild("buyerId")
                .equalTo(buyerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Ticket> tickets = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Ticket t = snapshotToTicket(child);
                            if (t != null) tickets.add(t);
                        }
                        java.util.Collections.reverse(tickets);
                        callback.onSuccess(tickets);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    /**
     * Marks a ticket as sold and records the buyer's UID.
     */
    public void markTicketSold(String ticketId, String buyerId, VoidCallback callback) {
        if (ticketId == null || ticketId.isEmpty()) {
            callback.onFailure("Invalid ticket ID");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_STATUS, STATUS_SOLD);
        updates.put("buyerId",    buyerId != null ? buyerId : "");

        database.getReference(PATH_TICKETS)
                .child(ticketId)
                .updateChildren(updates)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to update ticket"));
    }

    // ── Chat system ───────────────────────────────────────────────────────────

    /**
     * Builds a deterministic chat ID from two user IDs by sorting them
     * alphabetically and joining with "_". This ensures both users always
     * reference the same chat node regardless of who initiates.
     *
     * @param uid1 first user's UID
     * @param uid2 second user's UID
     * @return chatId string
     */
    public static String buildChatId(String uid1, String uid2) {
        if (uid1 == null) uid1 = "";
        if (uid2 == null) uid2 = "";
        // Sort so the ID is the same regardless of who calls first
        if (uid1.compareTo(uid2) <= 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    /**
     * Sends a message to chats/{chatId}/messages/{messageId}.
     *
     * @param chatId     the chat room ID (use buildChatId())
     * @param senderId   UID of the sender
     * @param receiverId UID of the receiver
     * @param messageText the message text
     * @param callback   result callback
     */
    public void sendMessage(String chatId, String senderId, String receiverId,
                            String messageText, VoidCallback callback) {
        if (chatId == null || chatId.isEmpty()) {
            callback.onFailure("Invalid chat ID");
            return;
        }
        if (messageText == null || messageText.trim().isEmpty()) {
            callback.onFailure("Message cannot be empty");
            return;
        }

        DatabaseReference messagesRef = database.getReference(PATH_CHATS)
                .child(chatId)
                .child(PATH_MESSAGES);

        // push() generates a unique, time-ordered message ID
        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            callback.onFailure("Failed to generate message ID");
            return;
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId",  messageId);
        messageMap.put("senderId",   senderId != null ? senderId : "");
        messageMap.put("receiverId", receiverId != null ? receiverId : "");
        messageMap.put("message",    messageText.trim());
        messageMap.put("timestamp",  System.currentTimeMillis());

        android.util.Log.d("FIREBASE", "sendMessage: chatId=" + chatId
                + " senderId=" + senderId + " messageId=" + messageId);

        messagesRef.child(messageId)
                .setValue(messageMap)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("FIREBASE", "sendMessage: success — messageId=" + messageId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FIREBASE", "sendMessage: failed — " + e.getMessage());
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Failed to send message");
                });
    }

    /**
     * Attaches a ChildEventListener to chats/{chatId}/messages.
     * onChildAdded fires once per existing message on attach, then once per
     * new message — no duplicates, no full-snapshot rebuilds.
     *
     * Returns the ChildEventListener so the caller can remove it in onDestroy().
     */
    public com.google.firebase.database.ChildEventListener listenForMessages(
            String chatId,
            String currentUserId,
            MessagesCallback callback) {

        DatabaseReference ref = database.getReference(PATH_CHATS)
                .child(chatId)
                .child(PATH_MESSAGES);

        com.google.firebase.database.ChildEventListener listener =
                new com.google.firebase.database.ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot,
                                             String previousChildName) {
                        ChatMessage msg = snapshotToChatMessage(snapshot, currentUserId);
                        if (msg != null) callback.onNewMessage(msg);
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                };

        ref.addChildEventListener(listener);
        return listener;
    }

    /**
     * Removes a previously attached ChildEventListener.
     * Call this in onDestroy() to prevent memory leaks.
     */
    public void removeMessageListener(String chatId,
                                       com.google.firebase.database.ChildEventListener listener) {
        if (chatId == null || listener == null) return;
        database.getReference(PATH_CHATS)
                .child(chatId)
                .child(PATH_MESSAGES)
                .removeEventListener(listener);
    }

    // Keep the old ValueEventListener overload for backward compatibility
    public void removeMessageListener(String chatId, ValueEventListener listener) {
        if (chatId == null || listener == null) return;
        database.getReference(PATH_CHATS)
                .child(chatId)
                .child(PATH_MESSAGES)
                .removeEventListener(listener);
    }

    // ── Storage uploads ───────────────────────────────────────────────────────

    /**
     * Uploads a file to Firebase Storage and returns the download URL.
     *
     * @param fileUri  local content URI
     * @param folder   storage folder, e.g. "event_images" or "ticket_files"
     * @param callback result callback with download URL on success
     */
    public void uploadFile(Uri fileUri, String folder, UploadCallback callback) {
        if (fileUri == null) {
            callback.onFailure("No file selected");
            return;
        }

        String fileName = folder + "/" + UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Upload failed"));
    }

    // ── Parsing helpers ───────────────────────────────────────────────────────

    /**
     * Converts a Realtime Database DataSnapshot (tickets/{ticketId}) into a Ticket.
     * Returns null if the snapshot is invalid or missing required fields.
     */
    public Ticket snapshotToTicket(DataSnapshot snapshot) {
        try {
            if (snapshot == null || !snapshot.exists()) return null;

            String ticketId  = safeString(snapshot, FIELD_TICKET_ID);
            String title     = safeString(snapshot, FIELD_TITLE);
            String eventDate = safeString(snapshot, FIELD_EVENT_DATE);
            String imageUrl  = safeString(snapshot, FIELD_IMAGE_URL);
            String sellerId  = safeString(snapshot, FIELD_SELLER_ID);
            String sellerName = safeString(snapshot, FIELD_SELLER_NAME);
            String status    = safeString(snapshot, FIELD_STATUS);
            String description = safeString(snapshot, FIELD_DESCRIPTION);

            int price = 0;
            Object priceObj = snapshot.child(FIELD_PRICE).getValue();
            if (priceObj instanceof Long)   price = ((Long) priceObj).intValue();
            if (priceObj instanceof Double) price = ((Double) priceObj).intValue();
            if (priceObj instanceof Integer) price = (Integer) priceObj;

            if (ticketId == null || ticketId.isEmpty()) {
                ticketId = snapshot.getKey();
            }

            // Build a Ticket using the full constructor
            Ticket t = new Ticket(
                    ticketId,
                    title != null ? title : "",
                    eventDate != null ? eventDate : "",
                    eventDate != null ? eventDate : "",
                    price,
                    price,
                    sellerName != null ? sellerName : "Seller",
                    sellerId != null ? sellerId : "",
                    4.5f,
                    status != null ? status : STATUS_AVAILABLE,
                    imageUrl != null ? imageUrl : "",
                    ""
            );
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a Realtime Database DataSnapshot (messages/{messageId}) into a ChatMessage.
     * Returns null if the snapshot is invalid.
     */
    private ChatMessage snapshotToChatMessage(DataSnapshot snapshot, String currentUserId) {
        try {
            if (snapshot == null || !snapshot.exists()) return null;

            String senderId = safeString(snapshot, "senderId");
            String message  = safeString(snapshot, "message");
            long   timestamp = 0;

            Object tsObj = snapshot.child("timestamp").getValue();
            if (tsObj instanceof Long)   timestamp = (Long) tsObj;
            if (tsObj instanceof Double) timestamp = ((Double) tsObj).longValue();

            if (message == null || message.isEmpty()) return null;

            String time = new java.text.SimpleDateFormat(
                    "hh:mm a", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp));

            boolean isSentByMe = currentUserId != null
                    && currentUserId.equals(senderId);

            // Pass timestamp so ChatMessage.equals() can deduplicate correctly
            return new ChatMessage(message, isSentByMe, time, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    /** Safely reads a String field from a DataSnapshot, returning null if absent. */
    private String safeString(DataSnapshot snapshot, String field) {
        Object val = snapshot.child(field).getValue();
        return val != null ? val.toString() : null;
    }
}
