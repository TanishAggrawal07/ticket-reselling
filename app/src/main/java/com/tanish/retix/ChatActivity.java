package com.tanish.retix;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "ReTixPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView tvSellerName;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private String sellerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        sellerName = getIntent().getStringExtra("seller_name");
        if (sellerName == null) sellerName = "Seller";

        initViews();
        setupRecyclerView();
        loadDummyMessages();
        setupListeners();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    private void initViews() {
        rvMessages  = findViewById(R.id.rv_messages);
        etMessage   = findViewById(R.id.et_message);
        btnSend     = findViewById(R.id.btn_send);
        btnBack     = findViewById(R.id.btn_back);
        tvSellerName = findViewById(R.id.tv_seller_name);
        tvSellerName.setText(sellerName);
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messages);
        rvMessages.setAdapter(chatAdapter);
    }

    private void loadDummyMessages() {
        messages.add(new ChatMessage("Hi! Is this ticket still available?", true, "10:02 AM"));
        messages.add(new ChatMessage("Yes, it's available! Are you interested?", false, "10:04 AM"));
        messages.add(new ChatMessage("Yes! Can you confirm the seat number?", true, "10:05 AM"));
        messages.add(new ChatMessage("It's Row C, Seat 14. Front section.", false, "10:06 AM"));
        messages.add(new ChatMessage("That sounds great. Is the price negotiable?", true, "10:08 AM"));
        messages.add(new ChatMessage("I can do ₹5,200 if you confirm today.", false, "10:09 AM"));
        messages.add(new ChatMessage("Deal! I'll secure it now through ReTix.", true, "10:10 AM"));
        messages.add(new ChatMessage("Perfect! Payment is protected via escrow. You're safe.", false, "10:11 AM"));

        chatAdapter.notifyDataSetChanged();
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        messages.add(new ChatMessage(text, true, time));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
        etMessage.setText("");

        // Simulate seller reply after a short delay
        rvMessages.postDelayed(() -> {
            messages.add(new ChatMessage("Thanks for your message! I'll get back to you shortly.", false,
                    new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date())));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
        }, 1200);
    }
}
