package com.tanish.retix;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ApiManager - Single helper class for all API operations.
 * Replaces FirebaseManager with Retrofit-based API calls.
 *
 * Database tables:
 *   users (id, email, password_hash, name, profile_image_url, wallet_balance, created_at)
 *   tickets (id, title, description, price, event_date, image_url, seller_id, status, buyer_id)
 *   transactions (id, user_id, type, amount, description, ticket_id, created_at)
 *   conversations (id, user1_id, user2_id, created_at, updated_at)
 *   messages (id, conversation_id, sender_id, receiver_id, message, created_at)
 */
public class ApiManager {

    // Path constants (kept for compatibility)
    public static final String PATH_USERS = "users";
    public static final String PATH_TICKETS = "tickets";
    public static final String PATH_CHATS = "chats";
    public static final String PATH_MESSAGES = "messages";
    public static final String PATH_MY_TICKETS = "myTickets";

    // Ticket field constants (kept for compatibility)
    public static final String FIELD_TICKET_ID = "ticketId";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_EVENT_DATE = "eventDate";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_SELLER_ID = "sellerId";
    public static final String FIELD_SELLER_NAME = "sellerName";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_STATUS = "status";

    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_SOLD = "sold";

    // Wallet constants
    public static final String PATH_TRANSACTIONS = "transactions";
    public static final String WALLET_BALANCE_KEY = "walletBalance";
    public static final String TXN_TYPE_CREDIT = "credit";
    public static final String TXN_TYPE_DEBIT = "debit";

    // Singleton
    private static ApiManager instance;
    private final ApiService apiService;
    private final TokenManager tokenManager;

    private ApiManager() {
        apiService = ApiClient.getService();
        tokenManager = ApiClient.getTokenManager();
    }

    public static synchronized ApiManager getInstance() {
        if (instance == null) instance = new ApiManager();
        return instance;
    }

    // Callback interfaces
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

    public interface UserProfileCallback {
        void onSuccess(String name, String email, String profileImageUrl);
        void onFailure(String errorMessage);
    }

    public interface BalanceCallback {
        void onSuccess(long balance, long pendingBalance);
        void onFailure(String errorMessage);
    }

    public interface MonthlyEarningsCallback {
        void onSuccess(List<ApiService.MonthlyEarning> earnings);
        void onFailure(String errorMessage);
    }

    public interface TransactionsCallback {
        void onSuccess(List<WalletTransaction> transactions);
        void onFailure(String errorMessage);
    }

    public interface MessageListCallback {
        void onSuccess(List<ChatMessage> messages);
        void onFailure(String errorMessage);
    }

    // Auth helpers
    public String getCurrentUserId() {
        return tokenManager.getUserId();
    }

    public String getCurrentUserName() {
        String name = tokenManager.getUserName();
        return name != null && !name.isEmpty() ? name : "User";
    }

    // User system
    public void saveUser(String uid, String name, String email, String profileImage, VoidCallback callback) {
        ApiService.UpdateProfileRequest request = new ApiService.UpdateProfileRequest(name, profileImage);

        apiService.updateProfile(request).enqueue(new Callback<ApiService.ProfileResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProfileResponse> call, Response<ApiService.ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Failed to save user");
                }
            }

            @Override
            public void onFailure(Call<ApiService.ProfileResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void fetchUserProfile(String uid, UserProfileCallback callback) {
        apiService.getProfile().enqueue(new Callback<ApiService.ProfileResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProfileResponse> call, Response<ApiService.ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ApiService.ProfileData data = response.body().data;
                    callback.onSuccess(data.name, data.email, data.profileImageUrl);
                } else {
                    callback.onFailure("Failed to fetch profile");
                }
            }

            @Override
            public void onFailure(Call<ApiService.ProfileResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // Wallet system
    public void getWalletBalance(String uid, BalanceCallback callback) {
        apiService.getBalance().enqueue(new Callback<ApiService.BalanceResponse>() {
            @Override
            public void onResponse(Call<ApiService.BalanceResponse> call, Response<ApiService.BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    long balance = response.body().data.balance;
                    long pendingBalance = response.body().data.pendingBalance;
                    callback.onSuccess(balance, pendingBalance);
                } else {
                    callback.onSuccess(0, 0);
                }
            }

            @Override
            public void onFailure(Call<ApiService.BalanceResponse> call, Throwable t) {
                callback.onSuccess(0, 0);
            }
        });
    }

    // Note: Real-time listening is replaced with polling in the UI layer
    public void listenToWalletBalance(String uid, BalanceCallback callback) {
        // Single fetch - polling is done in the fragment
        getWalletBalance(uid, callback);
    }

    public void removeBalanceListener(String uid, Object listener) {
        // No-op - no real-time listeners in API version
    }

    public void addMoney(String uid, long amount, String description, VoidCallback callback) {
        // This is now handled internally by the buy ticket endpoint
        // For manual credits, would need a separate admin endpoint
        callback.onSuccess();
    }

    public void deductMoney(String uid, long amount, String description, VoidCallback callback) {
        // This is now handled internally by the buy ticket endpoint
        callback.onSuccess();
    }

    public void fetchTransactions(String uid, TransactionsCallback callback) {
        apiService.getTransactions(100, 0).enqueue(new Callback<ApiService.TransactionsResponse>() {
            @Override
            public void onResponse(Call<ApiService.TransactionsResponse> call, Response<ApiService.TransactionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<WalletTransaction> transactions = new ArrayList<>();
                    for (ApiService.ApiTransaction txn : response.body().data.transactions) {
                        WalletTransaction wt = new WalletTransaction(
                                txn.description != null ? txn.description : "",
                                txn.createdAt,
                                (int) txn.amount,
                                WalletTransaction.STATUS_AVAILABLE,
                                txn.isDebit
                        );
                        transactions.add(wt);
                    }
                    callback.onSuccess(transactions);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiService.TransactionsResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // Ticket system
    public void saveTicket(String title, String description, int originalPrice, int price,
                           String eventDate, String imageUrl, String ticketFileUrl,
                           String sellerId, String sellerName,
                           VoidCallback callback) {
        ApiService.CreateTicketRequest request = new ApiService.CreateTicketRequest(
                title, description, originalPrice, price, eventDate, imageUrl, ticketFileUrl
        );

        apiService.createTicket(request).enqueue(new Callback<ApiService.CreateTicketResponse>() {
            @Override
            public void onResponse(Call<ApiService.CreateTicketResponse> call, Response<ApiService.CreateTicketResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Failed to save ticket");
                }
            }

            @Override
            public void onFailure(Call<ApiService.CreateTicketResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void fetchAvailableTickets(TicketsCallback callback) {
        apiService.getAvailableTickets(100, 0).enqueue(new Callback<ApiService.TicketsResponse>() {
            @Override
            public void onResponse(Call<ApiService.TicketsResponse> call, Response<ApiService.TicketsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(convertApiTickets(response.body().data.tickets));
                } else {
                    callback.onFailure("Failed to fetch tickets");
                }
            }

            @Override
            public void onFailure(Call<ApiService.TicketsResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void fetchMyListings(String sellerId, TicketsCallback callback) {
        apiService.getMyListings(100, 0).enqueue(new Callback<ApiService.TicketsResponse>() {
            @Override
            public void onResponse(Call<ApiService.TicketsResponse> call, Response<ApiService.TicketsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(convertApiTickets(response.body().data.tickets));
                } else {
                    callback.onFailure("Failed to fetch listings");
                }
            }

            @Override
            public void onFailure(Call<ApiService.TicketsResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void fetchMyPurchases(String buyerId, TicketsCallback callback) {
        apiService.getMyPurchases(100, 0).enqueue(new Callback<ApiService.TicketsResponse>() {
            @Override
            public void onResponse(Call<ApiService.TicketsResponse> call, Response<ApiService.TicketsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(convertApiTickets(response.body().data.tickets));
                } else {
                    callback.onFailure("Failed to fetch purchases");
                }
            }

            @Override
            public void onFailure(Call<ApiService.TicketsResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void markTicketSold(String ticketId, String buyerId, VoidCallback callback) {
        // This is now done via the buy ticket endpoint
        callback.onSuccess();
    }

    public void buyTicket(String ticketId, ApiManager.VoidCallback callback) {
        apiService.buyTicket(ticketId).enqueue(new Callback<ApiService.BuyTicketResponse>() {
            @Override
            public void onResponse(Call<ApiService.BuyTicketResponse> call, Response<ApiService.BuyTicketResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Failed to buy ticket");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BuyTicketResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void confirmEntry(String ticketId, VoidCallback callback) {
        apiService.confirmEntry(ticketId).enqueue(new Callback<ApiService.ConfirmEntryResponse>() {
            @Override
            public void onResponse(Call<ApiService.ConfirmEntryResponse> call, Response<ApiService.ConfirmEntryResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Failed to confirm entry");
                }
            }

            @Override
            public void onFailure(Call<ApiService.ConfirmEntryResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void getMonthlyEarnings(MonthlyEarningsCallback callback) {
        apiService.getMonthlyEarnings().enqueue(new Callback<ApiService.MonthlyEarningsResponse>() {
            @Override
            public void onResponse(Call<ApiService.MonthlyEarningsResponse> call, Response<ApiService.MonthlyEarningsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data.monthlyEarnings);
                } else {
                    callback.onFailure("Failed to fetch monthly earnings");
                }
            }

            @Override
            public void onFailure(Call<ApiService.MonthlyEarningsResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // Chat system
    public static String buildChatId(String uid1, String uid2) {
        if (uid1 == null) uid1 = "";
        if (uid2 == null) uid2 = "";
        return uid1.compareTo(uid2) <= 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public void sendMessage(String chatId, String senderId, String receiverId,
                            String messageText, VoidCallback callback) {
        ApiService.SendMessageRequest request = new ApiService.SendMessageRequest(receiverId, messageText);

        apiService.sendMessage(request).enqueue(new Callback<ApiService.SendMessageResponse>() {
            @Override
            public void onResponse(Call<ApiService.SendMessageResponse> call, Response<ApiService.SendMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Failed to send message");
                }
            }

            @Override
            public void onFailure(Call<ApiService.SendMessageResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void fetchMessages(String chatId, MessageListCallback callback) {
        apiService.getMessages(chatId, 100, 0).enqueue(new Callback<ApiService.MessagesResponse>() {
            @Override
            public void onResponse(Call<ApiService.MessagesResponse> call, Response<ApiService.MessagesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<ChatMessage> messages = new ArrayList<>();
                    for (ApiService.ApiMessage msg : response.body().data.messages) {
                        ChatMessage cm = new ChatMessage(
                                msg.message,
                                msg.isSentByMe,
                                formatTime(msg.createdAt),
                                parseTimestamp(msg.createdAt)
                        );
                        messages.add(cm);
                    }
                    callback.onSuccess(messages);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiService.MessagesResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // Note: Real-time listeners replaced with polling
    public Object listenForMessages(String chatId, String currentUserId, MessagesCallback callback) {
        // Return null - polling is done in the activity
        return null;
    }

    public void removeMessageListener(String chatId, Object listener) {
        // No-op - no real-time listeners
    }

    // Upload
    public void uploadFile(Uri fileUri, String folder, UploadCallback callback) {
        // This is now handled via the uploadImage endpoint
        // The actual file upload is done in the fragment
        callback.onFailure("Use uploadImage endpoint instead");
    }

    public void uploadImage(okhttp3.MultipartBody.Part imagePart, UploadCallback callback) {
        apiService.uploadImage(imagePart).enqueue(new Callback<ApiService.UploadResponse>() {
            @Override
            public void onResponse(Call<ApiService.UploadResponse> call, Response<ApiService.UploadResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data.url);
                } else {
                    callback.onFailure(response.body() != null && response.body().error != null
                            ? response.body().error.message : "Upload failed");
                }
            }

            @Override
            public void onFailure(Call<ApiService.UploadResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // Helper methods
    private List<Ticket> convertApiTickets(List<ApiService.ApiTicket> apiTickets) {
        List<Ticket> tickets = new ArrayList<>();
        for (ApiService.ApiTicket apiTicket : apiTickets) {
            Ticket t = new Ticket(
                    apiTicket.id,
                    apiTicket.title,
                    apiTicket.eventDate,
                    apiTicket.eventDate,
                    apiTicket.originalPrice > 0 ? apiTicket.originalPrice : apiTicket.price,
                    apiTicket.price,
                    apiTicket.sellerName != null ? apiTicket.sellerName : "Seller",
                    apiTicket.sellerId,
                    0f,
                    apiTicket.status,
                    apiTicket.buyerId != null ? apiTicket.buyerId : "",
                    apiTicket.imageUrl != null ? apiTicket.imageUrl : "",
                    apiTicket.ticketFileUrl != null ? apiTicket.ticketFileUrl : ""
            );
            tickets.add(t);
        }
        return tickets;
    }

    private String formatTime(String isoDate) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat(
                    "hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(new java.util.Date());
        }
    }

    private long parseTimestamp(String isoDate) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            return inputFormat.parse(isoDate).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
