package com.thomastriplett.capturenotes;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;

class GetEditDocTask extends AsyncTask<EditActivity.GetDocTaskParams, Void, Document> {

    private EditActivity editActivity;
    public GetEditDocTask(EditActivity ea) {
        editActivity = ea;
    }
    private Exception exception;

    @Override
    protected Document doInBackground(EditActivity.GetDocTaskParams... params) {
        try {
            Docs service = params[0].service;
            String docId = params[0].docId;
            Document response = service.documents().get(docId).execute();
            return response;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(Document result) {
        if (result == null) {
            Log.e("Exception", "File upload failed");
            Toast.makeText(editActivity, "Note Not Saved, Error Retrieving Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            editActivity.whenGetDocTaskIsDone(result);
        }
    }

}
