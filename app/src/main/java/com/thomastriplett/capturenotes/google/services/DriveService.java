package com.thomastriplett.capturenotes.google.services;

import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.thomastriplett.capturenotes.common.AuthManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DriveService {

    private static final String TAG = "In DriveService";

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Drive build() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Drive service successfully created");
            return driveService;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error building Google HTTP Transport for Drive service: " + e);
            return null;
        }
    }
}
