package com.thomastriplett.capturenotes;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Body;
import com.google.api.services.docs.v1.model.Document;
import com.google.auth.oauth2.GoogleCredentials;

class UpdateDocTask extends AsyncTask<ImageActivity.UpdateDocTaskParams, Void, BatchUpdateDocumentResponse> {

    private ImageActivity imageActivity;
    public UpdateDocTask(ImageActivity ia) {
        imageActivity = ia;
    }
    private Exception exception;

    @Override
    protected BatchUpdateDocumentResponse doInBackground(ImageActivity.UpdateDocTaskParams... params) {
        try {
            Docs service = params[0].service;
            String docId = params[0].docId;
            BatchUpdateDocumentRequest body = params[0].body;
            BatchUpdateDocumentResponse response = service.documents()
                    .batchUpdate(docId, body).execute();
            return response;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(BatchUpdateDocumentResponse result) {
        if (result == null) {
            Log.e("Exception", "File upload failed");
            Toast.makeText(imageActivity, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            imageActivity.whenUpdateDocTaskIsDone(result);
        }
    }

}
