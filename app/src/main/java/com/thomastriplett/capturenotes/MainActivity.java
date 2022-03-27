package com.thomastriplett.capturenotes;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView title;
    private ImageView notesButton;
    private ImageView imageButton;
    private final String TAG = "In MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notesButton = findViewById(R.id.notes_button);
        imageButton = findViewById(R.id.image_button);

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent notesIntent = new Intent(MainActivity.this, NotesActivity.class);
                startActivity(notesIntent);
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
