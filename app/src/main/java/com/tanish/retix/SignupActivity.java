package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ReTixPrefs";

    private EditText inputName;
    private EditText inputEmail;
    private EditText inputPassword;
    private TextView btnSignup;
    private TextView btnLogin;
    private TextView btnBack;
    private TextView errorName;
    private TextView errorEmail;
    private TextView errorPassword;
    private ProgressBar signupProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        setupTextWatchers();
        setupClickListeners();
    }

    private void initViews() {
        inputName     = findViewById(R.id.input_name);
        inputEmail    = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnSignup     = findViewById(R.id.btn_signup);
        btnLogin      = findViewById(R.id.btn_login);
        btnBack       = findViewById(R.id.btn_back);
        errorName     = findViewById(R.id.error_name);
        errorEmail    = findViewById(R.id.error_email);
        errorPassword = findViewById(R.id.error_password);
        signupProgress = findViewById(R.id.signup_progress);
    }

    private void setupTextWatchers() {
        inputName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorName.setVisibility(View.GONE);
            }
        });
        inputEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorEmail.setVisibility(View.GONE);
            }
        });
        inputPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorPassword.setVisibility(View.GONE);
            }
        });
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> attemptSignup());
        btnLogin.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
        String name     = inputName.getText().toString().trim();
        String email    = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        errorName.setVisibility(View.GONE);
        errorEmail.setVisibility(View.GONE);
        errorPassword.setVisibility(View.GONE);

        boolean isValid = true;

        if (name.isEmpty()) {
            errorName.setText(getString(R.string.signup_error_name_empty));
            errorName.setVisibility(View.VISIBLE);
            isValid = false;
        }
        if (email.isEmpty()) {
            errorEmail.setText(getString(R.string.signup_error_email_empty));
            errorEmail.setVisibility(View.VISIBLE);
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorEmail.setText(getString(R.string.signup_error_email_invalid));
            errorEmail.setVisibility(View.VISIBLE);
            isValid = false;
        }
        if (password.isEmpty()) {
            errorPassword.setText(getString(R.string.signup_error_password_empty));
            errorPassword.setVisibility(View.VISIBLE);
            isValid = false;
        } else if (password.length() < 6) {
            errorPassword.setText(getString(R.string.signup_error_password_short));
            errorPassword.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (isValid) performSignup(name, email, password);
    }

    private void performSignup(String name, String email, String password) {
        btnSignup.setText("");
        signupProgress.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            btnSignup.setText(getString(R.string.signup_button));
            signupProgress.setVisibility(View.GONE);
            btnSignup.setEnabled(true);

            Toast.makeText(this, R.string.signup_success, Toast.LENGTH_SHORT).show();
            finish(); // Return to login screen
        }, 1500);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public abstract void onTextChanged(CharSequence s, int start, int before, int count);
        @Override public void afterTextChanged(Editable s) {}
    }
}
