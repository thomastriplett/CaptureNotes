package com.thomastriplett.capturenotes;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;

class CreateEditDocTask extends AsyncTask<EditActivity.CreateDocTaskParams, Void, EditActivity.CreateDocTaskParams> {

    private EditActivity editActivity;
    public CreateEditDocTask(EditActivity ea) {
        editActivity = ea;
    }
    private Exception exception;

    @Override
    protected EditActivity.CreateDocTaskParams doInBackground(EditActivity.CreateDocTaskParams... params) {
        try {
            Document doc = params[0].doc;
            Docs service = params[0].service;
            doc = service.documents().create(doc)
                    .execute();
            EditActivity.CreateDocTaskParams result = new EditActivity.CreateDocTaskParams(doc,service);
            return result;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(EditActivity.CreateDocTaskParams result) {
        if (result == null) {
            Log.e("Exception", "Doc creation failed");
            Toast.makeText(editActivity, "Note Not Saved, Error Creating Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            editActivity.whenCreateDocTaskIsDone(result);
        }
    }

}
