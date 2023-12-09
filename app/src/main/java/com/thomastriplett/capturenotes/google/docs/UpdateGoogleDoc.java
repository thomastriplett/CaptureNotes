package com.thomastriplett.capturenotes.google.docs;

import android.util.Log;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.concurrent.Executor;

public class UpdateGoogleDoc {

    private final String TAG = "In UpdateGoogleDoc";

    public void execute(Docs service, String docId, BatchUpdateDocumentRequest body, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            BatchUpdateDocumentResponse response = null;
            try {
                response = service.documents().batchUpdate(docId, body).execute();
            } catch (Exception e) {
                Log.e(TAG,"Error in execute: "+ e);
            }

            // Invoke the callback on the main thread
            callback.onComplete(response);
        });
    }

    public void execute(Docs service, String docId, BatchUpdateDocumentRequest body, Drive driveService, File file, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            BatchUpdateDocumentResponse response = null;
            try {
                response = service.documents().batchUpdate(docId, body).execute();
                driveService.files().update(docId,file).execute();
            } catch (Exception e) {
                Log.e(TAG,"Error in execute: "+ e);
            }

            // Invoke the callback on the main thread
            callback.onComplete(response);
        });
    }

    public interface MyCallback {
        void onComplete(BatchUpdateDocumentResponse result);

    }
}
