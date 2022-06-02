package com.thomastriplett.capturenotes;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.nio.file.Files;
import java.nio.file.Paths;

class SendAudioTask extends AsyncTask<SpeechActivity.SendAudioTaskParams, Void, Blob> {

    private SpeechActivity speechActivity;
    public SendAudioTask(SpeechActivity sa) {
        speechActivity = sa;
    }
    private Exception exception;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected Blob doInBackground(SpeechActivity.SendAudioTaskParams... params) {
        try {
            Storage storage = params[0].storage;
            BlobInfo blobInfo = params[0].blobInfo;
            String filePath = params[0].filePath;
            Blob response = storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
            return response;
        } catch (Exception e) {
            Log.e("Exception","Error in doInBackground: "+e.toString());
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(Blob result) {
        if (result == null) {
            Log.e("Exception", "Audio file upload failed");
            Toast.makeText(speechActivity, "Audio file not uploaded, Error Uploading to Google Cloud", Toast.LENGTH_SHORT).show();
        }
        else {
            speechActivity.whenSendAudioTaskIsDone(result);
        }
    }

}
