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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private static final String PREFS_NAME       = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private EditText    inputName, inputEmail, inputPassword;
    private TextView    btnSignup, btnLogin, btnBack;
    private TextView    errorName, errorEmail, errorPassword;
    private ProgressBar signupProgress;

    private FirebaseAuth    mAuth;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth           = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupTextWatchers();
        setupClickListeners();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void initViews() {
        inputName      = findViewById(R.id.input_name);
        inputEmail     = findViewById(R.id.input_email);
        inputPassword  = findViewById(R.id.input_password);
        btnSignup      = findViewById(R.id.btn_signup);
        btnLogin       = findViewById(R.id.btn_login);
        btnBack        = findViewById(R.id.btn_back);
        errorName      = findViewById(R.id.error_name);
        errorEmail     = findViewById(R.id.error_email);
        errorPassword  = findViewById(R.id.error_password);
        signupProgress = findViewById(R.id.signup_progress);
    }

    // ── Text watchers ─────────────────────────────────────────────────────────

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

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> attemptSignup());
        btnLogin.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    // ── Validation ────────────────────────────────────────────────────────────

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

    // ── Firebase Auth signup ──────────────────────────────────────────────────

    private void performSignup(String name, String email, String password) {
        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserToDatabase(user.getUid(), name, email);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Account created. Please log in.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showSignupError(e);
                });
    }

    // ── Realtime Database: save user ──────────────────────────────────────────

    /**
     * Writes the new user's profile to users/{uid} in Realtime Database.
     * Also caches name/email locally for ProfileFragment.
     */
    private void saveUserToDatabase(String uid, String name, String email) {
        android.util.Log.d("FIREBASE", "saveUserToDatabase: writing users/" + uid
                + " name=" + name + " email=" + email);

        firebaseManager.saveUser(uid, name, email, "",
                new FirebaseManager.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);

                        // Persist login state for SplashActivity routing
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .apply();

                        // Cache name/email locally for ProfileFragment
                        getSharedPreferences(EditProfileActivity.PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(EditProfileActivity.KEY_PROFILE_NAME,  name)
                                .putString(EditProfileActivity.KEY_PROFILE_EMAIL, email)
                                .apply();

                        Toast.makeText(SignupActivity.this,
                                getString(R.string.signup_success),
                                Toast.LENGTH_SHORT).show();

                        navigateToHome();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        // Auth succeeded but DB write failed — still let the user in
                        Toast.makeText(SignupActivity.this,
                                "Account created. Profile will sync later.",
                                Toast.LENGTH_LONG).show();

                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .apply();

                        navigateToHome();
                    }
                });
    }

    // ── Error handling ────────────────────────────────────────────────────────

    private void showSignupError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        if (e instanceof FirebaseAuthUserCollisionException) {
            errorEmail.setText("An account with this email already exists");
            errorEmail.setVisibility(View.VISIBLE);
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            errorPassword.setText("Password is too weak. Use at least 6 characters");
            errorPassword.setVisibility(View.VISIBLE);
        } else if (message.contains("CONFIGURATION_NOT_FOUND")
                || message.contains("configuration-not-found")) {
            Toast.makeText(this,
                    "Firebase Auth not configured. Enable Email/Password in Firebase Console.",
                    Toast.LENGTH_LONG).show();
        } else if (message.contains("NETWORK_ERROR") || message.contains("network_error")) {
            Toast.makeText(this,
                    "Network error. Please check your internet connection.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Signup failed: " + message, Toast.LENGTH_LONG).show();
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (loading) {
            btnSignup.setText("");
            btnSignup.setEnabled(false);
            signupProgress.setVisibility(View.VISIBLE);
        } else {
            btnSignup.setText(getString(R.string.signup_button));
            btnSignup.setEnabled(true);
            signupProgress.setVisibility(View.GONE);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Simple TextWatcher helper ─────────────────────────────────────────────

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public abstract void onTextChanged(CharSequence s, int start, int before, int count);
        @Override public void afterTextChanged(Editable s) {}
    }
}
