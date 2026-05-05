package com.tanish.retix;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText     etMessage;
    private ImageButton  btnSend;
    private ImageButton  btnBack;
    private TextView     tvSellerName;

    private ChatAdapter       chatAdapter;
    private List<ChatMessage> messages;

    // Firebase
    private FirebaseManager firebaseManager;
    private String          currentUserId;
    private String          receiverId;
    private String          chatId;

    // ChildEventListener fires once per message — no duplicates
    private ChildEventListener messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        firebaseManager = FirebaseManager.getInstance();
        currentUserId   = firebaseManager.getCurrentUserId();

        String sellerName = getIntent().getStringExtra("seller_name");
        receiverId        = getIntent().getStringExtra("seller_id");

        if (sellerName == null || sellerName.isEmpty()) sellerName = "Seller";

        // Guard: if no sellerId passed (e.g. dummy data), use a placeholder
        if (receiverId == null || receiverId.isEmpty()) {
            receiverId = "unknown_seller";
        }

        chatId = FirebaseManager.buildChatId(
                currentUserId != null ? currentUserId : "guest", receiverId);

        initViews(sellerName);
        setupRecyclerView();
        setupListeners();
        listenForMessages();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent memory leaks
        if (messageListener != null) {
            firebaseManager.removeMessageListener(chatId, messageListener);
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews(String sellerName) {
        rvMessages   = findViewById(R.id.rv_messages);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        btnBack      = findViewById(R.id.btn_back);
        tvSellerName = findViewById(R.id.tv_seller_name);
        tvSellerName.setText(sellerName);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messages);
        rvMessages.setAdapter(chatAdapter);
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    // ── Real-time message listener ────────────────────────────────────────────

    /**
     * Uses ChildEventListener so onChildAdded fires exactly once per message —
     * once for each existing message on attach, then once per new message.
     * No duplicates, no full-snapshot rebuilds.
     */
    private void listenForMessages() {
        if (currentUserId == null) {
            messages.add(new ChatMessage(
                    "Please log in to chat with the seller.", false, getCurrentTime()));
            chatAdapter.notifyDataSetChanged();
            return;
        }

        messageListener = firebaseManager.listenForMessages(
                chatId, currentUserId,
                new FirebaseManager.MessagesCallback() {
                    @Override
                    public void onNewMessage(ChatMessage message) {
                        messages.add(message);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ChatActivity.this,
                                "Failed to load messages: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Send message ──────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to send messages",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText(""); // clear immediately for responsive feel

        firebaseManager.sendMessage(chatId, currentUserId, receiverId, text,
                new FirebaseManager.VoidCallback() {
                    @Override public void onSuccess() {
                        // ChildEventListener will display the message automatically
                    }
                    @Override public void onFailure(String errorMessage) {
                        Toast.makeText(ChatActivity.this,
                                "Failed to send: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                        etMessage.setText(text); // restore on failure
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }
}
