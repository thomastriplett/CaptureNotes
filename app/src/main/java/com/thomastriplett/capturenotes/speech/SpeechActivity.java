package com.thomastriplett.capturenotes.speech;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.MainActivity;
import com.thomastriplett.capturenotes.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.RECORD_AUDIO;

public class SpeechActivity extends AppCompatActivity{
    private ImageView recordButton;
    private ImageView stopButton;
    private ImageView saveButton;
    private TextView recordText;
    private TextView noteTitle;


    private MediaRecorder recorder;
    private static String fileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private Credentials credentials;
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    String objectName;

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    String projectId = "capturenotes";
    boolean recorderActive = false;

    private final String TAG = "SpeechActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recordButton = findViewById(R.id.record_button);
        stopButton = findViewById(R.id.stop_button);
        saveButton = findViewById(R.id.recording_save_button);
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
            @RequiresApi(api = Build.VERSION_CODES.O)
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
                Log.e("TAG", "prepare() failed");
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
    public void onBackPressed() {
        super.onBackPressed();
        if (recorderActive) {
            stopRecording();
        }
        Intent mainIntent = new Intent(SpeechActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 400 && resultCode == RESULT_OK) {
            Log.d(TAG,"Sign in result received");
            handleSignInIntent(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        GoogleAccountCredential credential = GoogleAccountCredential
                                .usingOAuth2(SpeechActivity.this, Collections.singleton(DocsScopes.DRIVE_FILE));

                        credential.setSelectedAccount(googleSignInAccount.getAccount());
                        onCredentialReceived(credential);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
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
            ArrayList<String> urlList = new ArrayList<>();
            urlList.add("https://www.googleapis.com/auth/cloud-platform");
            credentials = GoogleCredentials.fromStream(jsonStream);
        }
        catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    protected static class SendAudioTaskParams {
        Storage storage;
        BlobInfo blobInfo;
        String filePath;

        SendAudioTaskParams(Storage storage, BlobInfo blobInfo, String filePath) {
            this.storage = storage;
            this.blobInfo = blobInfo;
            this.filePath = filePath;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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

        SpeechActivity.SendAudioTaskParams params = new SpeechActivity.SendAudioTaskParams(storage, blobInfo, filePath);
        new SendAudioTask(SpeechActivity.this).execute(params);
        Log.d(TAG, "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }

    public void whenSendAudioTaskIsDone(Blob blob) {
        Log.d(TAG,"In whenSendAudioTaskIsDone");
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

    private void requestSignIn() {
        Log.d(TAG, "In requestSignIn");
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DocsScopes.DRIVE_FILE),new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this,signInOptions);

        startActivityForResult(client.getSignInIntent(),400);
    }

    private void saveNote(String recording) {

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        String title = noteTitle.getText().toString();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
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
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
        String date = dateFormat.format(new Date());

        dbHelper.saveNotes(username, title, recording, date, docId);

        Toast.makeText(SpeechActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();
    }

    private void onCredentialReceived(GoogleAccountCredential credential){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Document doc = new Document()
                    .setTitle(noteTitle.getText().toString());

            SpeechActivity.CreateDocTaskParams params = new SpeechActivity.CreateDocTaskParams(doc, service);
            new CreateSpeechDocTask(SpeechActivity.this).execute(params);

        } catch(IOException e) {
            Toast.makeText(SpeechActivity.this, "Note Not Uploaded", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
        }
        catch(GeneralSecurityException e) {
            Toast.makeText(SpeechActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
        }
    }

    protected static class CreateDocTaskParams {
        Document doc;
        Docs service;

        CreateDocTaskParams(Document doc, Docs service) {
            this.doc = doc;
            this.service = service;
        }
    }

    protected static class UpdateDocTaskParams {
        Docs service;
        String docId;
        BatchUpdateDocumentRequest body;

        UpdateDocTaskParams(Docs service, String docId, BatchUpdateDocumentRequest body) {
            this.service = service;
            this.docId = docId;
            this.body = body;
        }
    }


    public void whenCreateDocTaskIsDone(CreateDocTaskParams params) {
        Document doc = params.doc;
        Docs service = params.service;
        Log.d(TAG,"Created document with title: " + doc.getTitle());
        String docId = doc.getDocumentId();
        Log.d(TAG,"Document ID: " + docId);

        saveNote(recordText.getText().toString(), docId);

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setText(recordText.getText().toString())
                .setLocation(new Location().setIndex(1))));


        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
        SpeechActivity.UpdateDocTaskParams updateParams = new SpeechActivity.UpdateDocTaskParams(service,docId,body);
        new UpdateSpeechDocTask(this).execute(updateParams);
    }


    public void whenUpdateDocTaskIsDone(BatchUpdateDocumentResponse result) {
        Toast.makeText(SpeechActivity.this, "Note uploaded to Google Docs", Toast.LENGTH_SHORT).show();
    }

    private void save() {
        Log.d(TAG,"In save");
        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");
        if(saveLocation.equals("googleDocs")){
            requestSignIn();
        }
        else if(saveLocation.equals("appOnly")){
            saveNote(recordText.getText().toString());
        } else {
            Toast.makeText(this, "Error with Save Location", Toast.LENGTH_LONG).show();
        }
    }


}
