package com.tanish.retix;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * CloudinaryClient — OkHttp-based helper for unsigned image uploads.
 *
 * Uses a synchronous OkHttp call on a background thread, then posts the
 * result back to the main thread via Handler. This avoids the Retrofit
 * enqueue() + raw Thread() interaction that can silently drop callbacks
 * and cause infinite loading.
 *
 * Config (unsigned — no API secret required):
 *   Cloud name:    ddsqvkavb
 *   Upload preset: retix_upload  (must be "Unsigned" in Cloudinary Console)
 *   Folder:        retix_tickets
 */
public class CloudinaryClient {

    private static final String TAG = "UPLOAD";

    // ── Cloudinary config ─────────────────────────────────────────────────────
    private static final String CLOUD_NAME    = "ddsqvkavb";
    private static final String UPLOAD_PRESET = "retix_upload";
    private static final String FOLDER        = "retix_tickets";
    private static final String UPLOAD_URL    =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    // ── Timeouts ──────────────────────────────────────────────────────────────
    private static final int CONNECT_TIMEOUT_SEC = 30;
    private static final int READ_TIMEOUT_SEC    = 60;
    private static final int WRITE_TIMEOUT_SEC   = 60;
    private static final int CALL_TIMEOUT_SEC    = 90; // overall hard limit

    // ── Callback interface ────────────────────────────────────────────────────

    public interface UploadCallback {
        /** Called on the main thread with the Cloudinary secure_url. */
        void onSuccess(String secureUrl);
        /** Called on the main thread with a human-readable error message. */
        void onFailure(String errorMessage);
    }

    // ── Singleton OkHttpClient ────────────────────────────────────────────────

    private static OkHttpClient httpClient;

    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                    message -> Log.d(TAG, message));
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SEC,    TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT_SEC,  TimeUnit.SECONDS)
                    .callTimeout(CALL_TIMEOUT_SEC,    TimeUnit.SECONDS) // hard overall limit
                    .build();
        }
        return httpClient;
    }

    // ── Main handler for UI-thread callbacks ──────────────────────────────────

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Uploads an image to Cloudinary on a background thread.
     * Both onSuccess and onFailure are always delivered on the main thread.
     * The loading state is ALWAYS stopped — no infinite loading possible.
     *
     * @param context  Android context (used to open the content URI)
     * @param imageUri content URI of the image to upload
     * @param callback result callback (always called, always on main thread)
     */
    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        if (imageUri == null) {
            Log.e(TAG, "uploadImage: imageUri is null");
            MAIN.post(() -> callback.onFailure("No image selected"));
            return;
        }

        // Capture application context so we don't leak the Activity/Fragment
        final Context appContext = context.getApplicationContext();

        Log.d(TAG, "uploadImage: starting — uri=" + imageUri);

        new Thread(() -> {
            // ── 1. Read image bytes ───────────────────────────────────────────
            byte[] imageBytes;
            String mimeType;
            try {
                InputStream inputStream = appContext.getContentResolver()
                        .openInputStream(imageUri);
                if (inputStream == null) {
                    Log.e(TAG, "uploadImage: cannot open InputStream for uri=" + imageUri);
                    MAIN.post(() -> callback.onFailure("Cannot open image file"));
                    return;
                }
                imageBytes = readBytes(inputStream);
                inputStream.close();

                mimeType = appContext.getContentResolver().getType(imageUri);
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    mimeType = "image/jpeg";
                }

                Log.d(TAG, "uploadImage: read " + imageBytes.length
                        + " bytes, mimeType=" + mimeType);
            } catch (IOException e) {
                Log.e(TAG, "uploadImage: read failed — " + e.getMessage());
                MAIN.post(() -> callback.onFailure("Failed to read image: " + e.getMessage()));
                return;
            }

            if (imageBytes.length == 0) {
                Log.e(TAG, "uploadImage: image file is empty");
                MAIN.post(() -> callback.onFailure("Image file is empty"));
                return;
            }

            // ── 2. Build multipart request ────────────────────────────────────
            RequestBody fileBody = RequestBody.create(
                    imageBytes, MediaType.parse(mimeType));

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file",          "upload.jpg", fileBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .addFormDataPart("folder",        FOLDER)
                    .build();

            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "uploadImage: sending request to " + UPLOAD_URL);

            // ── 3. Execute synchronously (we're already on a background thread) ─
            try (Response response = getHttpClient().newCall(request).execute()) {

                String responseBody = response.body() != null
                        ? response.body().string() : "";

                Log.d(TAG, "uploadImage: HTTP " + response.code()
                        + " response=" + responseBody);

                if (!response.isSuccessful()) {
                    String errorMsg = "Upload failed (HTTP " + response.code() + "): "
                            + responseBody;
                    Log.e(TAG, errorMsg);
                    MAIN.post(() -> callback.onFailure(errorMsg));
                    return;
                }

                // ── 4. Parse JSON response ────────────────────────────────────
                try {
                    JSONObject json = new JSONObject(responseBody);

                    // Check for Cloudinary error object inside a 200 response
                    if (json.has("error")) {
                        String cloudError = json.getJSONObject("error")
                                .optString("message", "Unknown Cloudinary error");
                        Log.e(TAG, "uploadImage: Cloudinary error — " + cloudError);
                        MAIN.post(() -> callback.onFailure("Cloudinary error: " + cloudError));
                        return;
                    }

                    String secureUrl = json.optString("secure_url", "");
                    if (secureUrl.isEmpty()) {
                        Log.e(TAG, "uploadImage: secure_url missing in response");
                        MAIN.post(() -> callback.onFailure(
                                "Upload succeeded but no URL returned"));
                        return;
                    }

                    Log.d(TAG, "uploadImage: success — secure_url=" + secureUrl);
                    MAIN.post(() -> callback.onSuccess(secureUrl));

                } catch (org.json.JSONException e) {
                    Log.e(TAG, "uploadImage: JSON parse error — " + e.getMessage());
                    MAIN.post(() -> callback.onFailure(
                            "Failed to parse upload response: " + e.getMessage()));
                }

            } catch (IOException e) {
                // Network failure, timeout, or connection refused
                Log.e(TAG, "uploadImage: network error — " + e.getMessage());
                MAIN.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

        }, "cloudinary-upload-thread").start();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Reads all bytes from an InputStream into a byte array. */
    private static byte[] readBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
