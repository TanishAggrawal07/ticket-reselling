package com.tanish.retix;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient - Retrofit client for ReTix backend API.
 * Handles JWT authentication and request/response logging.
 */
public class ApiClient {

    private static final String PREFS_NAME = "ApiPrefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_BASE_URL = "https://backend-three-phi-61.vercel.app/api/";

    private static ApiService apiService;
    private static TokenManager tokenManager;

    /**
     * Initialize the API client with context.
     * Call this in Application.onCreate()
     */
    public static void init(Context context) {
        tokenManager = new TokenManager(context);
        createApiService(getBaseUrl(context));
    }

    /**
     * Get the API service instance.
     */
    public static ApiService getService() {
        if (apiService == null) {
            throw new IllegalStateException("ApiClient not initialized. Call init() first.");
        }
        return apiService;
    }

    /**
     * Set a custom base URL (useful for development/testing).
     */
    public static void setBaseUrl(Context context, String baseUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
        createApiService(baseUrl);
    }

    /**
     * Get the current base URL.
     */
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    /**
     * Get the TokenManager instance.
     */
    public static TokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * Create the Retrofit API service.
     */
    private static void createApiService(String baseUrl) {
        // Logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Auth interceptor to add JWT token
        Interceptor authInterceptor = new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request.Builder builder = originalRequest.newBuilder();

                // Add auth token if available
                if (tokenManager != null && tokenManager.hasToken()) {
                    builder.header("Authorization", "Bearer " + tokenManager.getToken());
                }

                // Only add content-type if not already set (multipart has its own)
                if (originalRequest.header("Content-Type") == null) {
                    builder.header("Content-Type", "application/json");
                }

                Request request = builder.build();
                return chain.proceed(request);
            }
        };

        // Build OkHttp client
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .build();

        // Build Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Check if the user is logged in (has a valid token).
     */
    public static boolean isLoggedIn() {
        return tokenManager != null && tokenManager.hasToken();
    }

    /**
     * Logout - clear stored token.
     */
    public static void logout() {
        if (tokenManager != null) {
            tokenManager.clearToken();
        }
    }
}
