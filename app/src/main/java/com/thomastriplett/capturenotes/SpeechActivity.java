package com.thomastriplett.capturenotes;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.gax.core.FixedCredentialsProvider;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

public class SpeechActivity extends AppCompatActivity{
    private TextView audioInstr;
    private ImageView recordButton;
    private ImageView stopButton;
    private ImageView saveButton;
    private TextView recordText;
    private TextView noteTitle;


    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static String mFileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    public static final int REQUEST_ONLY_AUDIO_PERMISSION_CODE = 2;

    private Credentials credentials;

    String projectId = "capturenotes";

    private final String TAG = "SpeechActivity";
    private boolean keepRecording = false;
    private ArrayList<String> currentRecording = new ArrayList<>();
    private  AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        audioInstr = findViewById(R.id.audio_instr);
        recordButton = findViewById(R.id.record_button);
        stopButton = findViewById(R.id.stop_button);
        saveButton = findViewById(R.id.save_button);
        recordText = findViewById(R.id.record_text);
        noteTitle = findViewById(R.id.note_title);


        // save the current volume
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // switch to silent mode (to avoid the speech recogniton beep)
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);


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
                stopRecording();
        }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                sendAudioToGoogleCloud();
//                writeToFile(recordText.getText().toString());
            }
        });
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        super.onStop();
    }

    private void writeToFile(String recording) {
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/saved_recordings");
            myDir.mkdirs();


            String fname = "srecord-"+noteTitle.getText()+".txt";
            File file = new File (myDir, fname);

            FileOutputStream fos = new FileOutputStream(file);

            fos.write(noteTitle.getText().toString().getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            SimpleDateFormat sd = new SimpleDateFormat("MM/dd/yy hh:mm a");
            Date newDate = new Date();
            String date = sd.format(newDate);
            fos.write(date.getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            fos.write(recording.getBytes());
            fos.close();
            Toast.makeText(SpeechActivity.this, "Note Saved", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Toast.makeText(SpeechActivity.this, "Note Not Saved, Try Changing the Title", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void startRecording() {
        Log.d(TAG, "In startRecording");
        if (checkPermissions()) {
            Log.d(TAG, "Permissions Found");
            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/AudioRecording.amr-wb";

            Log.d(TAG, "File path name: "+mFileName);

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            mRecorder.setOutputFile(mFileName);
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            }

            mRecorder.start();
            audioInstr.setText("Recording Started");
        } else {
            requestPermissions();
        }
    }

    public void stopRecording() {
        mRecorder.stop();

        mRecorder.release();
        mRecorder = null;
        audioInstr.setText("Recording Stopped");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    ActivityCompat.requestPermissions(SpeechActivity.this, new String[]{RECORD_AUDIO}, REQUEST_ONLY_AUDIO_PERMISSION_CODE);

                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // this method is called when user will
        // grant the permission for audio recording.
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case REQUEST_ONLY_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermissions() {
        // this method is used to check permission
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        // this method is used to request the
        // permission for audio recording and storage.
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(SpeechActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
        }
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
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        String bucketName = "capturenotes-audio-storage";
        String objectName = "capturenotes-audio-file-1";
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        String filePath = mFileName;

        SpeechActivity.SendAudioTaskParams params = new SpeechActivity.SendAudioTaskParams(storage, blobInfo, filePath);
        new SendAudioTask(SpeechActivity.this).execute(params);
        Log.d(TAG, "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }

    public void whenSendAudioTaskIsDone(Blob blob) {
        Log.d(TAG,"In whenSendAudioTaskIsDone");
        Toast.makeText(this, "Audio File Successfully sent to Google Cloud", Toast.LENGTH_SHORT).show();

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
            String uri = "gs://capturenotes-audio-storage/capturenotes-audio-file-1";
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
}
