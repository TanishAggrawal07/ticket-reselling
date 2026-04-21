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

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME       = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private EditText inputEmail;
    private EditText inputPassword;
    private TextView btnLogin;
    private TextView btnSignup;
    private TextView btnForgotPassword;
    private TextView errorEmail;
    private TextView errorPassword;
    private ProgressBar loginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupTextWatchers();
        setupClickListeners();
    }

    private void initViews() {
        inputEmail        = findViewById(R.id.input_email);
        inputPassword     = findViewById(R.id.input_password);
        btnLogin          = findViewById(R.id.btn_login);
        btnSignup         = findViewById(R.id.btn_signup);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
        errorEmail        = findViewById(R.id.error_email);
        errorPassword     = findViewById(R.id.error_password);
        loginProgress     = findViewById(R.id.login_progress);
    }

    private void setupTextWatchers() {
        inputEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorEmail.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorPassword.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSignup.setOnClickListener(v -> navigateToSignup());
        btnForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Password reset feature coming soon", Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {
        String email    = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        errorEmail.setVisibility(View.GONE);
        errorPassword.setVisibility(View.GONE);

        boolean isValid = true;

        if (email.isEmpty()) {
            errorEmail.setText(getString(R.string.login_error_email_empty));
            errorEmail.setVisibility(View.VISIBLE);
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorEmail.setText(getString(R.string.login_error_email_invalid));
            errorEmail.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (password.isEmpty()) {
            errorPassword.setText(getString(R.string.login_error_password_empty));
            errorPassword.setVisibility(View.VISIBLE);
            isValid = false;
        } else if (password.length() < 6) {
            errorPassword.setText(getString(R.string.login_error_password_short));
            errorPassword.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (isValid) performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        btnLogin.setText("");
        loginProgress.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            btnLogin.setText(getString(R.string.login_button));
            loginProgress.setVisibility(View.GONE);
            btnLogin.setEnabled(true);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();

            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToHome();
        }, 1500);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        startActivity(new Intent(this, SignupActivity.class));
    }
}
