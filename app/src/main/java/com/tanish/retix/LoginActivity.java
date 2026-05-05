package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

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

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupTextWatchers();
        setupClickListeners();
    }

    // ── View binding ──────────────────────────────────────────────────────────

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

    // ── Text watchers ─────────────────────────────────────────────────────────

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

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSignup.setOnClickListener(v -> navigateToSignup());
        btnForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Password reset feature coming soon",
                        Toast.LENGTH_SHORT).show());
    }

    // ── Validation ────────────────────────────────────────────────────────────

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

    // ── Firebase login ────────────────────────────────────────────────────────

    private void performLogin(String email, String password) {
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);

                    // Persist login state locally so SplashActivity can skip login
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_IS_LOGGED_IN, true)
                            .apply();

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showLoginError(e);
                });
    }

    // ── Error handling ────────────────────────────────────────────────────────

    private void showLoginError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        if (e instanceof FirebaseAuthInvalidUserException) {
            errorEmail.setText("No account found with this email");
            errorEmail.setVisibility(View.VISIBLE);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            errorPassword.setText("Incorrect password. Please try again");
            errorPassword.setVisibility(View.VISIBLE);
        } else if (message.contains("CONFIGURATION_NOT_FOUND")
                || message.contains("configuration-not-found")) {
            // Email/Password sign-in provider is not enabled in Firebase Console.
            // Go to: Firebase Console → Authentication → Sign-in method → Email/Password → Enable
            Toast.makeText(this,
                    "Firebase Auth is not configured. Please enable Email/Password sign-in in the Firebase Console.",
                    Toast.LENGTH_LONG).show();
        } else if (message.contains("NETWORK_ERROR")
                || message.contains("network_error")) {
            Toast.makeText(this,
                    "Network error. Please check your internet connection.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "Login failed: " + message,
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (loading) {
            btnLogin.setText("");
            btnLogin.setEnabled(false);
            loginProgress.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText(getString(R.string.login_button));
            btnLogin.setEnabled(true);
            loginProgress.setVisibility(View.GONE);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

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
