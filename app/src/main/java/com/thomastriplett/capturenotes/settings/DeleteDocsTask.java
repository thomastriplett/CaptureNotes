package com.thomastriplett.capturenotes.settings;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.thomastriplett.capturenotes.common.Note;

import java.util.ArrayList;

class DeleteDocsTask extends AsyncTask<SettingsActivity.DeleteDocsTaskParams, Void, String> {

    private SettingsActivity settingsActivity;
    public DeleteDocsTask(SettingsActivity sa) {
        settingsActivity = sa;
    }
    private Exception exception;

    @Override
    protected String doInBackground(SettingsActivity.DeleteDocsTaskParams... params) {
        try {
            Drive service = params[0].driveService;
            ArrayList<Note> googleDocNotes = params[0].docNotes;
            for(int i=0; i<googleDocNotes.size(); i++){
                Note note = googleDocNotes.get(i);
                service.files().delete(note.getDocId()).execute();
            }
            return "success";
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(String result) {
        if (result == null) {
            Log.e("Exception", "File deletion failed");
            Toast.makeText(settingsActivity, "Notes Not Deleted, Error Deleting Google Docs", Toast.LENGTH_SHORT).show();
        }
        else {
            settingsActivity.whenDeleteDocsTaskIsDone(result);
        }
    }

}
