package com.thomastriplett.capturenotes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView title;
    private ImageView speechButton;
    private ImageView imageButton;
    private final String TAG = "In MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = findViewById(R.id.camera_instr);
        speechButton = findViewById(R.id.speech_button);
        imageButton = findViewById(R.id.image_button);

        speechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent speechIntent = new Intent(MainActivity.this, SpeechActivity.class);
                startActivity(speechIntent);
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageIntent = new Intent(MainActivity.this, ImageActivity.class);
                startActivity(imageIntent);
            }
        });
    }

    /*
    public void whenAsyncIsDone(ArrayList<Note> notes) {
        //textView.setText(textView.getText().toString() + "\n" + string.toString() + " END")
        if (notes.size() != 0) {
            //allNotes.addAll(notes);
            for(int i = 0; i<notes.size(); i++) {
                Note currentNote = notes.get(i);
                if (!allNotes.contains(currentNote)) {
                    allNotes.add(currentNote);
                }
            }
        }
        nAdapter.notifyDataSetChanged();
        Log.d(TAG, String.valueOf(allNotes.size()));
        Log.d(TAG, "AsyncTask Complete");
    }
        */
}
