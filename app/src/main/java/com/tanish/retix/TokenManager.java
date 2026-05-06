package com.tanish.retix;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * TokenManager - Handles JWT token storage and retrieval.
 * Stores tokens securely in SharedPreferences.
 */
public class TokenManager {

    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save JWT token and user info after login/signup.
     */
    public void saveToken(String token, String userId, String email, String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    /**
     * Get the stored JWT token.
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Check if a token exists.
     */
    public boolean hasToken() {
        return getToken() != null;
    }

    /**
     * Get the current user ID.
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Get the current user email.
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Get the current user name.
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /**
     * Clear all stored auth data (logout).
     */
    public void clearToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_NAME);
        editor.apply();
    }

    /**
     * Update stored user name (after profile update).
     */
    public void updateUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    /**
     * Update stored user info (after profile update).
     */
    public void updateUserInfo(String name, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        if (name != null) editor.putString(KEY_USER_NAME, name);
        if (email != null) editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }
}
