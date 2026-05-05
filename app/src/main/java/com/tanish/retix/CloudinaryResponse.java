package com.tanish.retix;

import com.google.gson.annotations.SerializedName;

/**
 * Maps the JSON response from Cloudinary's upload API.
 *
 * Relevant fields from the response:
 * {
 *   "public_id":   "retix_tickets/abc123",
 *   "secure_url":  "https://res.cloudinary.com/ddsqvkavb/image/upload/...",
 *   "url":         "http://res.cloudinary.com/...",
 *   "format":      "jpg",
 *   "bytes":       123456,
 *   "error":       { "message": "..." }   ← only present on failure
 * }
 */
public class CloudinaryResponse {

    @SerializedName("public_id")
    private String publicId;

    @SerializedName("secure_url")
    private String secureUrl;

    @SerializedName("url")
    private String url;

    @SerializedName("format")
    private String format;

    @SerializedName("bytes")
    private long bytes;

    @SerializedName("error")
    private CloudinaryError error;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getPublicId()  { return publicId; }
    public String getSecureUrl() { return secureUrl; }
    public String getUrl()       { return url; }
    public String getFormat()    { return format; }
    public long   getBytes()     { return bytes; }

    /** Returns true if Cloudinary returned an error object in the response body. */
    public boolean hasError() {
        return error != null && error.getMessage() != null;
    }

    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    // ── Inner error class ─────────────────────────────────────────────────────

    public static class CloudinaryError {
        @SerializedName("message")
        private String message;

        public String getMessage() { return message; }
    }
}
