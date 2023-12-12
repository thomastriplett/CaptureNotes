package com.thomastriplett.capturenotes.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.DriveScopes;
import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.common.AuthManager;

import java.util.Collections;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "In MainActivity";

    ActivityResultLauncher<Intent> signInActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewGroup mainLayout = findViewById(R.id.mainActivityLayout);
        Objects.requireNonNull(getSupportActionBar()).setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar();
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View cView = getLayoutInflater().inflate(R.layout.activity_main_action_bar, mainLayout, false);
        getSupportActionBar().setCustomView(cView);

        ImageView notesButton = findViewById(R.id.notes_button);
        ImageView imageButton = findViewById(R.id.image_button);
        ImageView audioButton = findViewById(R.id.audio_button);

        ActionBar actionBar = getSupportActionBar();
        ImageButton settingsButton = actionBar.getCustomView().findViewById(R.id.settings_button);

        notesButton.setOnClickListener(v -> {
            Log.i(TAG, "Notes Button Clicked");
            Intent notesIntent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(notesIntent);
        });

        imageButton.setOnClickListener(v -> {
            Log.i(TAG, "Image Button Clicked");
            Intent imageIntent = new Intent(MainActivity.this, ImageActivity.class);
            startActivity(imageIntent);
        });

        audioButton.setOnClickListener(v -> {
            Log.i(TAG, "Audio Button Clicked");
            Intent imageIntent = new Intent(MainActivity.this, SpeechActivity.class);
            startActivity(imageIntent);
        });

        settingsButton.setOnClickListener(v -> {
            Log.i(TAG, "Settings Button Clicked");
            Intent intent2 = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent2);
        });

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");

        if(saveLocation.equals("")) {
            SharedPreferences.Editor editor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
            editor.putString("saveLocation", "googleDocs");
            editor.apply();
        }

        signInActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG,"Sign in result received");
                        Intent data = result.getData();
                        handleSignInIntent(data);
                    } else {
                        Log.d(TAG, "Sign in result received with bad result code");
                        Intent data = result.getData();
                        handleSignInIntent(data);
                    }
                });

        requestSignIn();
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DocsScopes.DRIVE_FILE),new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this,signInOptions);
        signInActivityResultLauncher.launch(client.getSignInIntent());
    }

    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(googleSignInAccount -> {
                    GoogleAccountCredential credential = GoogleAccountCredential
                            .usingOAuth2(MainActivity.this, Collections.singleton(DocsScopes.DRIVE_FILE));

                    credential.setSelectedAccount(googleSignInAccount.getAccount());
                    AuthManager.getInstance().setUserCredential(credential);
                })
                .addOnFailureListener(e -> Log.e("GoogleSignIn", "Sign-in failed.", e));
    }
}
