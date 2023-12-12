package com.thomastriplett.capturenotes.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.google.cloudstorage.UploadFile;
import com.thomastriplett.capturenotes.google.docs.CreateGoogleDoc;
import com.thomastriplett.capturenotes.google.docs.UpdateGoogleDoc;
import com.thomastriplett.capturenotes.google.services.DocsService;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.Manifest.permission.RECORD_AUDIO;

public class SpeechActivity extends AppCompatActivity{
    private TextView recordText;
    private TextView noteTitle;


    private MediaRecorder recorder;
    private static String fileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private Credentials credentials;
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    String objectName;
    private Docs docsService;
    Executor executor = Executors.newSingleThreadExecutor();
    CreateGoogleDoc createGoogleDoc = new CreateGoogleDoc();
    UpdateGoogleDoc updateGoogleDoc = new UpdateGoogleDoc();
    UploadFile uploadFile = new UploadFile();

    String projectId = "capturenotes";
    boolean recorderActive = false;

    private final String TAG = "SpeechActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);
        Objects.requireNonNull(getSupportActionBar()).setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ImageView recordButton = findViewById(R.id.record_button);
        ImageView stopButton = findViewById(R.id.stop_button);
        ImageView saveButton = findViewById(R.id.recording_save_button);
        recordText = findViewById(R.id.record_text);
        noteTitle = findViewById(R.id.note_title);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        getCredentials();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorderActive) {
                    stopRecording();
                    sendAudioToGoogleCloud();
                } else {
                    Toast.makeText(SpeechActivity.this, "Not currently recording", Toast.LENGTH_SHORT).show();
                }
        }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (recorderActive) {
                    stopRecording();
                }
                Intent mainIntent = new Intent(SpeechActivity.this, MainActivity.class);
                startActivity(mainIntent);
            }
        });

        docsService = DocsService.build();
    }

    private void startRecording() {
        Log.d(TAG, "In startRecording");
        if (checkPermissions()) {
            Log.d(TAG, "Permissions Found");
            fileName = SpeechActivity.this.getFilesDir().getAbsolutePath();
            fileName += "/AudioRecording.amr-wb";

            Log.d(TAG, "File path name: "+ fileName);

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            recorder.setOutputFile(fileName);
            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e(TAG, "prepare() failed");
            }

            recorder.start();
            recorderActive = true;
            Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions();
        }
    }

    public void stopRecording() {
        recorder.stop();
        recorderActive = false;
        recorder.release();
        recorder = null;
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // this method is called when user will
        // grant the permission for audio recording.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (permissionToRecord) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Microphone Permission Needed", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermissions() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(SpeechActivity.this, new String[]{RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
    }

     void getCredentials() {
        try {
            AssetManager assetManager = SpeechActivity.this.getAssets();
            InputStream jsonStream = assetManager.open("credentials.json");
            credentials = GoogleCredentials.fromStream(jsonStream);
        }
        catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void sendAudioToGoogleCloud() {
        Log.d(TAG, "credentials: "+credentials);
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        String bucketName = "capturenotes-audio-storage";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US);
        Date date = new Date();
        String timestamp = dateFormat.format(new Timestamp(date.getTime()));
        objectName = "capturenotes-audio-file-"+timestamp;
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        String filePath = fileName;

        uploadFile.execute(storage, blobInfo, filePath, executor, uploadFileResult -> {
            if (uploadFileResult == null) {
                Log.e(TAG, "Error Uploading Audio Recording File to Google Cloud Storage");
                runOnUiThread(() -> Toast.makeText(SpeechActivity.this, "Error converting recorded speech to text", Toast.LENGTH_SHORT).show());
            } else {
                Log.i(TAG, "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
                try {
                    SpeechSettings speechSettings = SpeechSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

                    SpeechClient speechClient = SpeechClient.create(speechSettings);
                    RecognitionConfig.AudioEncoding encoding = RecognitionConfig.AudioEncoding.AMR_WB;
                    int sampleRateHertz = 16000;
                    String languageCode = "en-US";
                    RecognitionConfig config = RecognitionConfig.newBuilder()
                            .setEncoding(encoding)
                            .setSampleRateHertz(sampleRateHertz)
                            .setLanguageCode(languageCode)
                            .build();
                    String uri = "gs://capturenotes-audio-storage/"+objectName;
                    RecognitionAudio audio = RecognitionAudio.newBuilder()
                            .setUri(uri)
                            .build();
                    RecognizeResponse response = speechClient.recognize(config, audio);
                    List<SpeechRecognitionResult> results = response.getResultsList();
                    Log.d(TAG,"Number of results = "+results.size());
                    for (SpeechRecognitionResult result : results) {
                        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                        recordText.setText(alternative.getTranscript());
                    }
                    speechClient.shutdown();
                }
                catch (IOException e) {
                    Log.e("Exception", "Audio recording failed: " + e.toString());
                }
            }
        });
    }

    private void saveNote(String recording) {

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        String title = noteTitle.getText().toString();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
        String date = dateFormat.format(new Date());

        dbHelper.saveNotes(username, title, recording, date, "None");

        Toast.makeText(SpeechActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();
    }

    private void saveNote(String recording, String docId) {

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        String title = noteTitle.getText().toString();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
        String date = dateFormat.format(new Date());

        dbHelper.saveNotes(username, title, recording, date, docId);

        runOnUiThread(() -> Toast.makeText(SpeechActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show());
    }

    private void uploadNoteToGoogleDocs(){
        Document doc = new Document()
                .setTitle(noteTitle.getText().toString());
        createGoogleDoc.execute(docsService, doc, executor, createGoogleDocResult -> {
            // Handle the result on the main thread
            if (createGoogleDocResult == null) {
                Log.e(TAG, "Doc creation failed");
                runOnUiThread(() -> Toast.makeText(SpeechActivity.this, "Note Not Saved, Error Creating Google Doc", Toast.LENGTH_SHORT).show());
            }
            else {
                Log.i(TAG,"Created document with title: " + createGoogleDocResult.getTitle());
                String docId = createGoogleDocResult.getDocumentId();
                Log.i(TAG,"Document ID: " + docId);

                saveNote(recordText.getText().toString(), docId);

                List<Request> requests = new ArrayList<>();
                requests.add(new Request().setInsertText(new InsertTextRequest()
                        .setText(recordText.getText().toString())
                        .setLocation(new Location().setIndex(1))));


                BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
                updateGoogleDoc.execute(docsService, docId, body, executor, updateGoogleDocResult -> {
                    // Handle the result on the main thread
                    if (updateGoogleDocResult == null) {
                        Log.e(TAG, "Error Adding Text to Google Doc");
                        runOnUiThread(() -> Toast.makeText(SpeechActivity.this, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show());
                    }
                    else {
                        runOnUiThread(() -> Toast.makeText(SpeechActivity.this, "Note uploaded to Google Docs", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
    }

    private void save() {
        Log.i(TAG,"In save");
        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");
        if(saveLocation.equals("googleDocs")){
            uploadNoteToGoogleDocs();
        }
        else if(saveLocation.equals("appOnly")){
            saveNote(recordText.getText().toString());
        } else {
            Toast.makeText(this, "Error with Save Location", Toast.LENGTH_LONG).show();
        }
    }


}
