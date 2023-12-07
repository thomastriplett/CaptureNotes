package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.thomastriplett.capturenotes.image.ImageActivity;
import com.thomastriplett.capturenotes.notes.NotesActivity;
import com.thomastriplett.capturenotes.settings.SettingsActivity;
import com.thomastriplett.capturenotes.speech.SpeechActivity;

public class MainActivity extends AppCompatActivity {
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

        ImageView notesButton = findViewById(R.id.notes_button);
        ImageView imageButton = findViewById(R.id.image_button);
        ImageView audioButton = findViewById(R.id.audio_button);

        ActionBar actionBar = getSupportActionBar();
        ImageButton settingsButton = actionBar.getCustomView().findViewById(R.id.sync_button);

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Notes Button Clicked");
                Intent notesIntent = new Intent(MainActivity.this, NotesActivity.class);
                startActivity(notesIntent);
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Image Button Clicked");
                Intent imageIntent = new Intent(MainActivity.this, ImageActivity.class);
                startActivity(imageIntent);
            }
        });

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Audio Button Clicked");
                Intent imageIntent = new Intent(MainActivity.this, SpeechActivity.class);
                startActivity(imageIntent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Settings Button Clicked");
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
