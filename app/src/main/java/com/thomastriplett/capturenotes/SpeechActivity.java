package com.thomastriplett.capturenotes;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class SpeechActivity extends AppCompatActivity {
    private TextView audioInstr;
    private ImageView recordButton;
    private ImageView stopButton;
    private ImageView saveButton;
    private TextView recordText;
    private TextView noteTitle;


    private SpeechRecognizer speechRecognizer;
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

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new listener());

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(keepRecording){
                    return;
                }
                try {
                    if (ActivityCompat.checkSelfPermission(SpeechActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(SpeechActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                                0);

                    } else {
                        //beginRecording();

                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

                        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
                        speechRecognizer.startListening(intent);
                        keepRecording = true;

                        //displaySpeechRecognizer();
                    }

                    audioInstr.setText("Recording");
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionAsString = sw.toString();
                    audioInstr.setText("Error"+e.toString());
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!keepRecording){
                    return;
                }
            try {
                keepRecording = false;
                speechRecognizer.cancel();
                audioInstr.setText("Stopped");

                String recording = "";
                for(int i = 0; i < currentRecording.size(); i++){
                    String currentString = currentRecording.get(i);
                    recording += currentString;
                    recording += " ";
                }
                Random rand = new Random();
                int n = rand.nextInt();
                noteTitle.setText("img"+n);
                recordText.setText(recording);

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                audioInstr.setText("Error"+e.toString());
                Log.d("here","dd",e);
            }

        }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToFile(recordText.getText().toString());
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

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            if(error == 6 || error == 7){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
                speechRecognizer.startListening(intent);
            }
            else {
                Log.d(TAG, "error " + error);
                audioInstr.setText("error " + error);
                keepRecording = false;
                speechRecognizer.cancel();
            }
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
                str += data.get(i);
            }

            currentRecording.add((String)data.get(0));

            if(keepRecording){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
                speechRecognizer.startListening(intent);
            }
        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
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


}
