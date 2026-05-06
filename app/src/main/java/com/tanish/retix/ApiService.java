package com.tanish.retix;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * ApiService - Retrofit interface for ReTix API endpoints.
 */
public interface ApiService {

    // ========== Auth Endpoints ==========

    @POST("auth/signup")
    Call<AuthResponse> signup(@Body SignupRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<TokenResponse> refreshToken(@Body RefreshRequest request);

    // ========== User Endpoints ==========

    @GET("users/profile")
    Call<ProfileResponse> getProfile();

    @PUT("users/profile")
    Call<ProfileResponse> updateProfile(@Body UpdateProfileRequest request);

    @GET("users/stats")
    Call<StatsResponse> getUserStats();

    // ========== Ticket Endpoints ==========

    @GET("tickets")
    Call<TicketsResponse> getAvailableTickets(@Query("limit") int limit, @Query("offset") int offset);

    @GET("tickets/{id}")
    Call<TicketDetailResponse> getTicket(@Path("id") String ticketId);

    @POST("tickets/create")
    Call<CreateTicketResponse> createTicket(@Body CreateTicketRequest request);

    @GET("tickets/my-listings")
    Call<TicketsResponse> getMyListings(@Query("limit") int limit, @Query("offset") int offset);

    @GET("tickets/my-purchases")
    Call<TicketsResponse> getMyPurchases(@Query("limit") int limit, @Query("offset") int offset);

    @POST("tickets/buy/{id}")
    Call<BuyTicketResponse> buyTicket(@Path("id") String ticketId);

    @POST("tickets/{id}/confirm-entry")
    Call<ConfirmEntryResponse> confirmEntry(@Path("id") String ticketId);

    // ========== Wallet Endpoints ==========

    @GET("wallet/balance")
    Call<BalanceResponse> getBalance();

    @GET("wallet/transactions")
    Call<TransactionsResponse> getTransactions(@Query("limit") int limit, @Query("offset") int offset);

    @GET("wallet/monthly-earnings")
    Call<MonthlyEarningsResponse> getMonthlyEarnings();

    // ========== Chat Endpoints ==========

    @GET("chat/conversations")
    Call<ConversationsResponse> getConversations();

    @GET("chat/messages/{chatId}")
    Call<MessagesResponse> getMessages(@Path("chatId") String chatId, @Query("limit") int limit, @Query("offset") int offset);

    @POST("chat/send")
    Call<SendMessageResponse> sendMessage(@Body SendMessageRequest request);

    // ========== Upload Endpoints ==========

    @Multipart
    @POST("upload/image")
    Call<UploadResponse> uploadImage(@Part MultipartBody.Part image);

    // ========== Request/Response Classes ==========

    class SignupRequest {
        String email;
        String password;
        String name;

        SignupRequest(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    class LoginRequest {
        String email;
        String password;

        LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class RefreshRequest {
        String token;

        RefreshRequest(String token) {
            this.token = token;
        }
    }

    class UpdateProfileRequest {
        String name;
        String profileImageUrl;

        UpdateProfileRequest(String name, String profileImageUrl) {
            this.name = name;
            this.profileImageUrl = profileImageUrl;
        }
    }

    class CreateTicketRequest {
        String title;
        String description;
        int originalPrice;
        int price;
        String eventDate;
        String imageUrl;
        String ticketFileUrl;

        CreateTicketRequest(String title, String description, int originalPrice, int price, String eventDate, String imageUrl, String ticketFileUrl) {
            this.title = title;
            this.description = description;
            this.originalPrice = originalPrice;
            this.price = price;
            this.eventDate = eventDate;
            this.imageUrl = imageUrl;
            this.ticketFileUrl = ticketFileUrl;
        }
    }

    class SendMessageRequest {
        String receiverId;
        String message;

        SendMessageRequest(String receiverId, String message) {
            this.receiverId = receiverId;
            this.message = message;
        }
    }

    // ========== Response Classes ==========

    class AuthResponse {
        boolean success;
        AuthData data;
        ApiError error;
    }

    class AuthData {
        String token;
        UserData user;
    }

    class UserData {
        String id;
        String email;
        String name;
        long walletBalance;
        String profileImageUrl;
    }

    class TokenResponse {
        boolean success;
        TokenData data;
        ApiError error;
    }

    class TokenData {
        String token;
    }

    class ProfileResponse {
        boolean success;
        ProfileData data;
        ApiError error;
    }

    class ProfileData {
        String id;
        String email;
        String name;
        String profileImageUrl;
        long walletBalance;
        String createdAt;
    }

    class StatsResponse {
        boolean success;
        StatsData data;
        ApiError error;
    }

    class StatsData {
        int activeListings;
        int ticketsSold;
        long totalEarnings;
    }

    class TicketsResponse {
        boolean success;
        TicketsData data;
        ApiError error;
    }

    class TicketsData {
        List<ApiTicket> tickets;
        int count;
    }

    class ApiTicket {
        String id;
        String title;
        String description;
        int originalPrice;
        int price;
        String eventDate;
        String imageUrl;
        String ticketFileUrl;
        String sellerId;
        String sellerName;
        String status;
        String buyerId;
        String createdAt;
        String updatedAt;
        boolean isOwner;
    }

    class TicketDetailResponse {
        boolean success;
        TicketDetailData data;
        ApiError error;
    }

    class TicketDetailData {
        ApiTicket ticket;
    }

    class CreateTicketResponse {
        boolean success;
        CreateTicketData data;
        ApiError error;
    }

    class CreateTicketData {
        ApiTicket ticket;
    }

    class BuyTicketResponse {
        boolean success;
        BuyTicketData data;
        ApiError error;
    }

    class BuyTicketData {
        String message;
        String ticketId;
        int price;
        long newBalance;
    }

    class BalanceResponse {
        boolean success;
        BalanceData data;
        ApiError error;
    }

    class BalanceData {
        long balance;
        long pendingBalance;
    }

    class TransactionsResponse {
        boolean success;
        TransactionsData data;
        ApiError error;
    }

    class TransactionsData {
        List<ApiTransaction> transactions;
        int count;
    }

    class ApiTransaction {
        String id;
        String type;
        long amount;
        String description;
        String ticketId;
        String ticketTitle;
        String createdAt;
        boolean isCredit;
        boolean isDebit;
    }

    class ConversationsResponse {
        boolean success;
        ConversationsData data;
        ApiError error;
    }

    class ConversationsData {
        List<ApiConversation> conversations;
    }

    class ApiConversation {
        String id;
        String otherUserId;
        String otherUserName;
        String lastMessage;
        String lastMessageAt;
        String createdAt;
    }

    class MessagesResponse {
        boolean success;
        MessagesData data;
        ApiError error;
    }

    class MessagesData {
        List<ApiMessage> messages;
        int count;
    }

    class ApiMessage {
        String id;
        String senderId;
        String senderName;
        String receiverId;
        String message;
        String createdAt;
        boolean isSentByMe;
    }

    class SendMessageResponse {
        boolean success;
        SendMessageData data;
        ApiError error;
    }

    class SendMessageData {
        ApiMessage message;
    }

    class UploadResponse {
        boolean success;
        UploadData data;
        ApiError error;
    }

    class UploadData {
        String url;
        String filename;
    }

    class ApiError {
        String code;
        String message;
    }

    class ConfirmEntryResponse {
        boolean success;
        ConfirmEntryData data;
        ApiError error;
    }

    class ConfirmEntryData {
        String message;
        String ticketId;
    }

    class MonthlyEarningsResponse {
        boolean success;
        MonthlyEarningsData data;
        ApiError error;
    }

    class MonthlyEarningsData {
        List<MonthlyEarning> monthlyEarnings;
    }

    class MonthlyEarning {
        String label;
        long total;
    }
}
