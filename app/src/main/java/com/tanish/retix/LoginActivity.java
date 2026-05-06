package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tanish.retix.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity - Handles user authentication using the ReTix API.
 * Replaces Firebase Auth with JWT-based authentication.
 * Uses ViewBinding for type-safe view access.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize API client
        if (ApiClient.getTokenManager() == null) {
            ApiClient.init(getApplicationContext());
        }

        setupTextWatchers();
        setupClickListeners();
    }

    // Text watchers - clear errors on input
    private void setupTextWatchers() {
        binding.inputEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.errorEmail.setVisibility(android.view.View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.inputPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.errorPassword.setVisibility(android.view.View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // Click listeners
    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnSignup.setOnClickListener(v -> navigateToSignup());
        binding.btnForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Password reset feature coming soon",
                        Toast.LENGTH_SHORT).show());
    }

    // Validation
    private void attemptLogin() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        binding.errorEmail.setVisibility(android.view.View.GONE);
        binding.errorPassword.setVisibility(android.view.View.GONE);

        boolean isValid = true;

        if (email.isEmpty()) {
            binding.errorEmail.setText(getString(R.string.login_error_email_empty));
            binding.errorEmail.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.errorEmail.setText(getString(R.string.login_error_email_invalid));
            binding.errorEmail.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.errorPassword.setText(getString(R.string.login_error_password_empty));
            binding.errorPassword.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        } else if (password.length() < 6) {
            binding.errorPassword.setText(getString(R.string.login_error_password_short));
            binding.errorPassword.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        }

        if (isValid) performLogin(email, password);
    }

    // API login
    private void performLogin(String email, String password) {
        setLoading(true);

        ApiService.LoginRequest request = new ApiService.LoginRequest(email, password);

        ApiClient.getService().login(request).enqueue(new Callback<ApiService.AuthResponse>() {
            @Override
            public void onResponse(Call<ApiService.AuthResponse> call, Response<ApiService.AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ApiService.AuthData data = response.body().data;

                    // Save token and user info
                    ApiClient.getTokenManager().saveToken(
                            data.token,
                            data.user.id,
                            data.user.email,
                            data.user.name
                    );

                    // Persist login state
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_IS_LOGGED_IN, true)
                            .apply();

                    // Cache name/email for ProfileFragment
                    getSharedPreferences(EditProfileActivity.PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(EditProfileActivity.KEY_PROFILE_NAME, data.user.name)
                            .putString(EditProfileActivity.KEY_PROFILE_EMAIL, data.user.email)
                            .apply();

                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                } else {
                    // Handle API error
                    if (response.body() != null && response.body().error != null) {
                        showLoginError(response.body().error.code, response.body().error.message);
                    } else {
                        showLoginError("SERVER_ERROR", "Login failed. Please try again.");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.AuthResponse> call, Throwable t) {
                setLoading(false);
                showLoginError("NETWORK_ERROR", "Network error. Please check your connection.");
            }
        });
    }

    // Error handling
    private void showLoginError(String code, String message) {
        switch (code) {
            case "INVALID_CREDENTIALS":
                binding.errorEmail.setText("Invalid email or password");
                binding.errorEmail.setVisibility(android.view.View.VISIBLE);
                break;
            case "USER_NOT_FOUND":
                binding.errorEmail.setText("No account found with this email");
                binding.errorEmail.setVisibility(android.view.View.VISIBLE);
                break;
            case "NETWORK_ERROR":
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Login failed: " + message, Toast.LENGTH_LONG).show();
                break;
        }
    }

    // UI helpers
    private void setLoading(boolean loading) {
        if (loading) {
            binding.btnLogin.setText("");
            binding.btnLogin.setEnabled(false);
            binding.loginProgress.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.btnLogin.setText(getString(R.string.login_button));
            binding.btnLogin.setEnabled(true);
            binding.loginProgress.setVisibility(android.view.View.GONE);
        }
    }

    // Navigation
    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        startActivity(new Intent(this, SignupActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
