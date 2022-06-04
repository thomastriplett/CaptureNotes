package com.thomastriplett.capturenotes.speech;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.thomastriplett.capturenotes.speech.SpeechActivity;

class CreateSpeechDocTask extends AsyncTask<SpeechActivity.CreateDocTaskParams, Void, SpeechActivity.CreateDocTaskParams> {

    private SpeechActivity speechActivity;
    public CreateSpeechDocTask(SpeechActivity sa) {
        speechActivity = sa;
    }
    private Exception exception;

    @Override
    protected SpeechActivity.CreateDocTaskParams doInBackground(SpeechActivity.CreateDocTaskParams... params) {
        try {
            Document doc = params[0].doc;
            Docs service = params[0].service;
            doc = service.documents().create(doc)
                    .execute();
            SpeechActivity.CreateDocTaskParams result = new SpeechActivity.CreateDocTaskParams(doc,service);
            return result;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(SpeechActivity.CreateDocTaskParams result) {
        if (result == null) {
            Log.e("Exception", "Doc creation failed");
            Toast.makeText(speechActivity, "Note Not Saved, Error Creating Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            speechActivity.whenCreateDocTaskIsDone(result);
        }
    }

}
