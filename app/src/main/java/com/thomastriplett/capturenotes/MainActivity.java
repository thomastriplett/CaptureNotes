package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView title;
    private ImageView notesButton;
    private ImageView imageButton;
    private ImageView audioButton;
    private ImageButton settingsButton;
    private final String TAG = "In MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayOptions(getSupportActionBar().DISPLAY_SHOW_CUSTOM);
        View cView = getLayoutInflater().inflate(R.layout.activity_main_action_bar, null);
        getSupportActionBar().setCustomView(cView);

        notesButton = findViewById(R.id.notes_button);
        imageButton = findViewById(R.id.image_button);
        audioButton = findViewById(R.id.audio_button);
        settingsButton = findViewById(R.id.sync_button);

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

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageIntent = new Intent(MainActivity.this, SpeechActivity.class);
                startActivity(imageIntent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent2);
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");

        if(saveLocation.equals("")) {
            SharedPreferences.Editor editor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
            editor.putString("saveLocation", "googleDocs");
            editor.apply();
        }
    }
}
