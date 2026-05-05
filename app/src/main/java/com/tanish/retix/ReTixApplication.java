package com.tanish.retix;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/**
 * Custom Application class.
 *
 * Explicitly calls FirebaseApp.initializeApp() so Firebase is fully
 * initialized before any Activity or Service tries to use it.
 * This prevents CONFIGURATION_NOT_FOUND and similar startup errors.
 */
public class ReTixApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase explicitly.
        // FirebaseApp reads google-services.json (processed by the
        // google-services Gradle plugin) to configure all Firebase services.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
