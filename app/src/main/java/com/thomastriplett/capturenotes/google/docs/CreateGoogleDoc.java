package com.thomastriplett.capturenotes.google.docs;

import android.util.Log;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;

import java.util.concurrent.Executor;

public class CreateGoogleDoc {

    private final String TAG = "In CreateGoogleDoc";

    public void execute(Docs service, Document doc, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            Document response = null;
            try {
                response = service.documents().create(doc).execute();
            } catch (Exception e) {
                Log.e(TAG,"Error in execute: "+ e);
            }

            // Invoke the callback on the main thread
            callback.onComplete(response);
        });
    }

    public interface MyCallback {
        void onComplete(Document result);

    }
}
