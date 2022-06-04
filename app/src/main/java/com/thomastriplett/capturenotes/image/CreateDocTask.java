package com.thomastriplett.capturenotes.image;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.thomastriplett.capturenotes.image.ImageActivity;

class CreateDocTask extends AsyncTask<ImageActivity.CreateDocTaskParams, Void, ImageActivity.CreateDocTaskParams> {

    private ImageActivity imageActivity;
    public CreateDocTask(ImageActivity ia) {
        imageActivity = ia;
    }
    private Exception exception;

    @Override
    protected ImageActivity.CreateDocTaskParams doInBackground(ImageActivity.CreateDocTaskParams... params) {
        try {
            Document doc = params[0].doc;
            Docs service = params[0].service;
            doc = service.documents().create(doc)
                    .execute();
            ImageActivity.CreateDocTaskParams result = new ImageActivity.CreateDocTaskParams(doc,service);
            return result;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(ImageActivity.CreateDocTaskParams result) {
        if (result == null) {
            Log.e("Exception", "Doc creation failed");
            Toast.makeText(imageActivity, "Note Not Saved, Error Creating Google Doc", Toast.LENGTH_SHORT).show();
        }
        else {
            imageActivity.whenCreateDocTaskIsDone(result);
        }
    }

}
