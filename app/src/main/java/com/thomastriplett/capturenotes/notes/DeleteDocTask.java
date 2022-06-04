package com.thomastriplett.capturenotes.notes;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.thomastriplett.capturenotes.notes.NotesActivity;

class DeleteDocTask extends AsyncTask<NotesActivity.DeleteDocTaskParams, Void, String> {

    private NotesActivity notesActivity;
    public DeleteDocTask(NotesActivity na) {
        notesActivity = na;
    }
    private Exception exception;

    @Override
    protected String doInBackground(NotesActivity.DeleteDocTaskParams... params) {
        try {
            Drive service = params[0].driveService;
            String docId = params[0].docId;
            service.files().delete(docId).execute();
            return docId;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(String result) {
        if (result == null) {
            Log.e("Exception", "File deletion failed");
            Toast.makeText(notesActivity, "Note Not Deleted, Error Deleting Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            notesActivity.whenDeleteDocTaskIsDone(result);
        }
    }

}
