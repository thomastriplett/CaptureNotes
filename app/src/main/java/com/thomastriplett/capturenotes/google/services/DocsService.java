package com.thomastriplett.capturenotes.google.services;

import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.thomastriplett.capturenotes.common.AuthManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DocsService {

    private static final String TAG = "In DocsService";

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Docs build() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Docs docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG, "Docs service successfully created");
            return docsService;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error building Google HTTP Transport for Docs service: " + e);
            return null;
        }
    }
}
