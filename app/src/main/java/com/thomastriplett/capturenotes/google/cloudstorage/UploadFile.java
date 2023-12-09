package com.thomastriplett.capturenotes.google.cloudstorage;

import android.util.Log;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

public class UploadFile {

    private final String TAG = "In CreateGoogleDoc";

    public void execute(Storage storage, BlobInfo blobInfo, String filePath, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            Blob response = null;
            try {
                response = storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
            } catch (Exception e) {
                Log.e(TAG,"Error in execute: "+ e);
            }

            // Invoke the callback on the main thread
            callback.onComplete(response);
        });
    }

    public interface MyCallback {
        void onComplete(Blob result);

    }
}
