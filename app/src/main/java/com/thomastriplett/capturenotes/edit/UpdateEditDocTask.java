package com.thomastriplett.capturenotes.edit;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.thomastriplett.capturenotes.edit.EditActivity;

class UpdateEditDocTask extends AsyncTask<EditActivity.UpdateDocTaskParams, Void, String> {

    private EditActivity editActivity;
    public UpdateEditDocTask(EditActivity ea) {
        editActivity = ea;
    }
    private Exception exception;

    @Override
    protected String doInBackground(EditActivity.UpdateDocTaskParams... params) {
        try {
            Docs service = params[0].service;
            String docId = params[0].docId;
            BatchUpdateDocumentRequest body = params[0].body;
            BatchUpdateDocumentResponse response = service.documents()
                    .batchUpdate(docId, body).execute();
            return docId;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(String result) {
        if (result == null) {
            Log.e("Exception", "File upload failed");
            Toast.makeText(editActivity, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            editActivity.whenUpdateDocTaskIsDone(result);
        }
    }

}
