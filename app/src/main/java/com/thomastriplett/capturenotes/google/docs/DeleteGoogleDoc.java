package com.thomastriplett.capturenotes.google.docs;

import android.util.Log;

import com.google.api.services.drive.Drive;
import com.thomastriplett.capturenotes.common.Note;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeleteGoogleDoc {

    private final String TAG = "In DeleteGoogleDoc";

    public void execute(Drive service, String docId, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            boolean success = true;
            try {
                service.files().delete(docId).execute();
            } catch (Exception e) {
                Log.e(TAG,"Error in execute: "+ e);
                success = false;
            }

            // Invoke the callback on the main thread
            callback.onComplete(success);
        });
    }

    public void execute(Drive service, ArrayList<Note> googleDocNotes, Executor executor, MyCallback callback) {
        executor.execute(() -> {
            // Perform background operation here
            AtomicBoolean success = new AtomicBoolean(true);
            googleDocNotes.forEach((note -> {
                try {
                    service.files().delete(note.getDocId()).execute();
                } catch (Exception e) {
                    Log.e(TAG,"Error in execute: "+ e);
                    success.set(false);
                }
            }));

            // Invoke the callback on the main thread
            callback.onComplete(success.get());
        });
    }

    public interface MyCallback {
        void onComplete(boolean result);

    }
}
