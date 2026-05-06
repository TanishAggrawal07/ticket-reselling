package com.tanish.retix;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChatActivity - Chat interface for buyers and sellers.
 * Uses API polling for messages instead of Firebase real-time.
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView tvSellerName;
    private ProgressBar progressBar;
    private View emptyStateView;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    private ApiManager apiManager;
    private String currentUserId;
    private String receiverId;
    private String receiverName;
    private String chatId;

    // Polling for new messages
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final long POLL_INTERVAL_MS = 3000; // Poll every 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        apiManager = ApiManager.getInstance();
        currentUserId = ApiClient.getTokenManager().getUserId();

        // Support both buyer and seller chat
        receiverName = getIntent().getStringExtra("receiver_name");
        if (receiverName == null) {
            receiverName = getIntent().getStringExtra("seller_name"); // legacy support
        }
        receiverId = getIntent().getStringExtra("receiver_id");
        if (receiverId == null) {
            receiverId = getIntent().getStringExtra("seller_id"); // legacy support
        }

        if (receiverName == null || receiverName.isEmpty()) receiverName = "Seller";

        // Guard: Check for self-chat
        if (receiverId != null && receiverId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Guard: if no sellerId passed
        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Invalid seller", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatId = ApiManager.buildChatId(currentUserId != null ? currentUserId : "guest", receiverId);

        initViews(receiverName);
        setupRecyclerView();
        setupListeners();
        showLoading(true);
        loadMessages(); // Load immediately
        startPolling();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume polling when returning to the chat
        if (pollRunnable == null) {
            startPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause polling when leaving the chat
        stopPolling();
    }

    // View binding
    private void initViews(String sellerName) {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        tvSellerName = findViewById(R.id.tv_seller_name);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateView = findViewById(R.id.empty_state);
        tvSellerName.setText(sellerName);
    }

    // RecyclerView
    private void setupRecyclerView() {
        messages = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messages);
        rvMessages.setAdapter(chatAdapter);
    }

    // Button listeners
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    // Polling for new messages
    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessages();
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

    // Load messages from API
    private void loadMessages() {
        if (currentUserId == null) {
            showLoading(false);
            return;
        }

        apiManager.fetchMessages(chatId, new ApiManager.MessageListCallback() {
            @Override
            public void onSuccess(List<ChatMessage> newMessages) {
                showLoading(false);

                // Update messages
                messages.clear();
                messages.addAll(newMessages);
                chatAdapter.notifyDataSetChanged();

                if (messages.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                // Show error only on initial load, not during polling
                if (messages.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        rvMessages.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // Send message
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to send messages",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText(""); // clear immediately for responsive feel

        // Add message to UI immediately (optimistic update)
        String time = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                .format(new java.util.Date());
        ChatMessage optimisticMessage = new ChatMessage(text, true, time);
        messages.add(optimisticMessage);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);

        apiManager.sendMessage(chatId, currentUserId, receiverId, text,
                new ApiManager.VoidCallback() {
                    @Override public void onSuccess() {
                        // Message sent successfully - it will appear on next poll
                    }

                    @Override public void onFailure(String errorMessage) {
                        Toast.makeText(ChatActivity.this,
                                "Failed to send: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                        // Remove optimistic message on failure
                        messages.remove(optimisticMessage);
                        chatAdapter.notifyDataSetChanged();
                        etMessage.setText(text); // restore on failure
                    }
                });
    }
}
