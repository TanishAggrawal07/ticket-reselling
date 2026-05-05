package com.tanish.retix;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit interface for Cloudinary's unsigned image upload REST API.
 *
 * Endpoint:
 *   POST https://api.cloudinary.com/v1_1/{cloudName}/image/upload
 *
 * Required multipart fields:
 *   file          — the image bytes
 *   upload_preset — the unsigned preset name (no API secret needed)
 *
 * Optional multipart fields:
 *   folder        — destination folder in Cloudinary
 */
public interface CloudinaryApiService {

    /**
     * Uploads an image to Cloudinary using an unsigned upload preset.
     *
     * @param cloudName    your Cloudinary cloud name (path variable)
     * @param file         the image as a multipart part
     * @param uploadPreset the unsigned upload preset name
     * @param folder       destination folder (e.g. "retix_tickets")
     * @return a Retrofit Call wrapping the CloudinaryResponse
     */
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    Call<CloudinaryResponse> uploadImage(
            @Path("cloudName")    String cloudName,
            @Part                 MultipartBody.Part file,
            @Part("upload_preset") RequestBody uploadPreset,
            @Part("folder")        RequestBody folder
    );
}
