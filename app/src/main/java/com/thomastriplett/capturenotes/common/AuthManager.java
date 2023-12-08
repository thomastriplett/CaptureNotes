package com.thomastriplett.capturenotes.common;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class AuthManager {
    private static AuthManager instance;
    private GoogleAccountCredential userCredential;

    private AuthManager() {
        // private constructor to prevent instantiation
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public GoogleAccountCredential getUserCredential() {
        return userCredential;
    }

    public void setUserCredential(GoogleAccountCredential credential) {
        this.userCredential = credential;
    }
}
