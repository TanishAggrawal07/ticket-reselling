package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tanish.retix.databinding.ActivitySignupBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SignupActivity - Handles user registration using the ReTix API.
 * Replaces Firebase Auth with JWT-based authentication.
 * Uses ViewBinding for type-safe view access.
 */
public class SignupActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ReTixPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize API client
        if (ApiClient.getTokenManager() == null) {
            ApiClient.init(getApplicationContext());
        }

        setupTextWatchers();
        setupClickListeners();
    }

    // Text watchers
    private void setupTextWatchers() {
        binding.inputName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.errorName.setVisibility(android.view.View.GONE);
            }
        });
        binding.inputEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.errorEmail.setVisibility(android.view.View.GONE);
            }
        });
        binding.inputPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.errorPassword.setVisibility(android.view.View.GONE);
            }
        });
    }

    // Click listeners
    private void setupClickListeners() {
        binding.btnSignup.setOnClickListener(v -> attemptSignup());
        binding.btnLogin.setOnClickListener(v -> finish());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    // Validation
    private void attemptSignup() {
        String name = binding.inputName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        binding.errorName.setVisibility(android.view.View.GONE);
        binding.errorEmail.setVisibility(android.view.View.GONE);
        binding.errorPassword.setVisibility(android.view.View.GONE);

        boolean isValid = true;

        if (name.isEmpty()) {
            binding.errorName.setText(getString(R.string.signup_error_name_empty));
            binding.errorName.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        }

        if (email.isEmpty()) {
            binding.errorEmail.setText(getString(R.string.signup_error_email_empty));
            binding.errorEmail.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.errorEmail.setText(getString(R.string.signup_error_email_invalid));
            binding.errorEmail.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.errorPassword.setText(getString(R.string.signup_error_password_empty));
            binding.errorPassword.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        } else if (password.length() < 6) {
            binding.errorPassword.setText(getString(R.string.signup_error_password_short));
            binding.errorPassword.setVisibility(android.view.View.VISIBLE);
            isValid = false;
        }

        if (isValid) performSignup(name, email, password);
    }

    // API signup
    private void performSignup(String name, String email, String password) {
        setLoading(true);

        ApiService.SignupRequest request = new ApiService.SignupRequest(email, password, name);

        ApiClient.getService().signup(request).enqueue(new Callback<ApiService.AuthResponse>() {
            @Override
            public void onResponse(Call<ApiService.AuthResponse> call, Response<ApiService.AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ApiService.AuthData data = response.body().data;

                    Log.d("SIGNUP", "Signup successful for user: " + data.user.id);

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

                    Toast.makeText(SignupActivity.this,
                            getString(R.string.signup_success),
                            Toast.LENGTH_SHORT).show();

                    navigateToHome();
                } else {
                    // Handle API error
                    if (response.body() != null && response.body().error != null) {
                        showSignupError(response.body().error.code, response.body().error.message);
                    } else {
                        showSignupError("SERVER_ERROR", "Signup failed. Please try again.");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.AuthResponse> call, Throwable t) {
                setLoading(false);
                Log.e("SIGNUP", "Signup failed: " + t.getMessage());
                showSignupError("NETWORK_ERROR", "Network error. Please check your connection.");
            }
        });
    }

    // Error handling
    private void showSignupError(String code, String message) {
        switch (code) {
            case "EMAIL_EXISTS":
                binding.errorEmail.setText("An account with this email already exists");
                binding.errorEmail.setVisibility(android.view.View.VISIBLE);
                break;
            case "WEAK_PASSWORD":
                binding.errorPassword.setText("Password is too weak. Use at least 6 characters");
                binding.errorPassword.setVisibility(android.view.View.VISIBLE);
                break;
            case "INVALID_EMAIL":
                binding.errorEmail.setText("Please provide a valid email address");
                binding.errorEmail.setVisibility(android.view.View.VISIBLE);
                break;
            case "NETWORK_ERROR":
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Signup failed: " + message, Toast.LENGTH_LONG).show();
                break;
        }
    }

    // UI helpers
    private void setLoading(boolean loading) {
        if (loading) {
            binding.btnSignup.setText("");
            binding.btnSignup.setEnabled(false);
            binding.signupProgress.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.btnSignup.setText(getString(R.string.signup_button));
            binding.btnSignup.setEnabled(true);
            binding.signupProgress.setVisibility(android.view.View.GONE);
        }
    }

    // Navigation
    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Simple TextWatcher helper
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public abstract void onTextChanged(CharSequence s, int start, int before, int count);
        @Override public void afterTextChanged(Editable s) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
