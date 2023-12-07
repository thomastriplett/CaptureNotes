package com.thomastriplett.capturenotes.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.common.Note;
import com.thomastriplett.capturenotes.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;

public class SettingsActivity extends AppCompatActivity {

    private final String TAG = "In SettingsActivity";

    public static ArrayList<Note> notes = new ArrayList<>();
    public static ArrayList<Note> googleDocNotes = new ArrayList<>();
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    Context context;
    String username;

    TextView googleAccountDescription;
    TextView saveLocationDescription;
    TextView notesCreatedDescription;
    TextView appVersionDescription;
    TextView privacyPolicyHeader;

    Button signOutButton;
    Button saveLocationButton;
    Button deleteNotesButton;

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        googleAccountDescription = findViewById(R.id.googleAccountDescription);
        saveLocationDescription = findViewById(R.id.saveLocationDescription);
        notesCreatedDescription = findViewById(R.id.notesCreatedDescription);
        appVersionDescription = findViewById(R.id.appVersionDescription);
        privacyPolicyHeader = findViewById(R.id.privacyPolicyHeader);

        signOutButton = findViewById(R.id.signOutButton);
        saveLocationButton = findViewById(R.id.saveLocationButton);
        deleteNotesButton = findViewById(R.id.deleteNotesButton);

        String packageName = getPackageName();
        try {
            appVersionDescription.setText(getPackageManager().getPackageInfo(packageName,0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username","");
        String saveLocation = sharedPreferences.getString("saveLocation","");
        context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        notes = dbHelper.readNotes(username);
        Log.i(TAG, "Read "+notes.size()+" notes from DB");
        googleDocNotes = (ArrayList<Note>) notes.clone();
        googleDocNotes.removeIf(note -> note.getDocId().equals("None"));

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null){
            googleAccountDescription.setText(account.getEmail());
        }

        if(saveLocation.equals("googleDocs")){
            saveLocationDescription.setText(R.string.save_location_google_docs);
        }
        else if(saveLocation.equals("appOnly")){
            saveLocationDescription.setText(R.string.save_location_app_only);
        }
        else {
            saveLocationDescription.setText(saveLocation);
        }

        int googleDocNoteCount = googleDocNotes.size();
        int localNoteCount = notes.size() - googleDocNoteCount;

        Log.i(TAG, googleDocNoteCount+" Google Doc Notes");
        Log.i(TAG, localNoteCount+" Local Notes");

        if(googleDocNoteCount == 1 && localNoteCount == 1){
            notesCreatedDescription.setText(googleDocNoteCount+" note in Google Docs\n"+localNoteCount+" note in app only");
        }
        else if(googleDocNoteCount == 1) {
            notesCreatedDescription.setText(googleDocNoteCount+" note in Google Docs\n"+localNoteCount+" notes in app only");
        }
        else if(localNoteCount == 1) {
            notesCreatedDescription.setText(googleDocNoteCount+" notes in Google Docs\n"+localNoteCount+" note in app only");
        }
        else {
            notesCreatedDescription.setText(googleDocNoteCount+" notes in Google Docs\n"+localNoteCount+" notes in app only");
        }

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSignOut();
            }
        });

        registerForContextMenu(findViewById(R.id.saveLocationButton));
        findViewById(R.id.saveLocationButton)
                .setOnClickListener(
                        view -> {
                            Log.d(TAG, "Save button clicked");
                            view.showContextMenu();
                        });

        deleteNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this, R.style.AlertDialogCustom);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (googleDocNotes.size() > 0){
                            deleteNotes();
                            createGoogleDocsDeletionToast();
                        }
                        else {
                            deleteNotes();
                        }

                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do Nothing
                    }
                });
                builder.setMessage("Are you sure you want to delete all notes?");
                builder.setTitle("Delete Notes");
                AlertDialog dialog =  builder.create();
                dialog.show();
            }
        });

        privacyPolicyHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPrivacyPolicy(v);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.saveLocationButton) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.save_button_context_menu, menu);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        SharedPreferences.Editor sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
        switch(item.getItemId()) {
            case R.id.saveInGoogleDocsItem:
                sharedPreferences.putString("saveLocation", "googleDocs");
                sharedPreferences.apply();
                Log.d(TAG,"Save Location Changed to Google Docs");
                saveLocationDescription.setText(R.string.save_location_google_docs);
                return true;
            case R.id.saveOnlyInAppItem:
                sharedPreferences.putString("saveLocation", "appOnly");
                sharedPreferences.apply();
                Log.d(TAG,"Save Location Changed to App Only");
                saveLocationDescription.setText(R.string.save_location_app_only);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void requestSignOut() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DocsScopes.DRIVE_FILE),new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this,signInOptions);

        client.signOut().addOnSuccessListener(new OnSuccessListener<Void>(){
            @Override
            public void onSuccess(Void unused) {
                googleAccountDescription.setText(R.string.not_signed_in);
                Toast.makeText(SettingsActivity.this, "Successfully Signed Out", Toast.LENGTH_SHORT).show();
            }
                })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SettingsActivity.this, "Error Logging Out", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteNotes() {
        notes.clear();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);
        dbHelper.deleteNotes(username);

        notesCreatedDescription.setText("0 notes");

        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putInt("noteCount",0);
        sharedPreferencesEditor.apply();
        Toast.makeText(SettingsActivity.this, "All notes deleted", Toast.LENGTH_SHORT).show();
    }

    private void createGoogleDocsDeletionToast() {
        AlertDialog.Builder secondBuilder = new AlertDialog.Builder(SettingsActivity.this, R.style.AlertDialogCustom);
        secondBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int j) {
                requestSignIn();
            }
        });
        secondBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int j) {
                // Do Nothing
            }
        });
        secondBuilder.setMessage("Delete copies in Google Docs too?");
        secondBuilder.setTitle("Delete Notes in Google Docs");
        AlertDialog dialog =  secondBuilder.create();
        dialog.show();
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DocsScopes.DRIVE_FILE),new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this,signInOptions);

        startActivityForResult(client.getSignInIntent(),400);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 400 && resultCode == RESULT_OK) {
            Log.d(TAG,"Sign in result received");
            handleSignInIntent(data);
        } else {
            Log.d(TAG,"Unknown result received, requestCode = "+requestCode);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        GoogleAccountCredential credential = GoogleAccountCredential
                                .usingOAuth2(SettingsActivity.this, Collections.singleton(DocsScopes.DRIVE_FILE));

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

    private void onCredentialReceived(GoogleAccountCredential credential){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Drive service created");

            SettingsActivity.DeleteDocsTaskParams updateParams = new SettingsActivity.DeleteDocsTaskParams(googleDocNotes, driveService);
            new DeleteDocsTask(this).execute(updateParams);


        } catch(IOException e) {
            Toast.makeText(SettingsActivity.this, "Google Docs copies not deleted", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File deletion failed: " + e.toString());
        }
        catch(GeneralSecurityException e) {
            Toast.makeText(SettingsActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File deletion failed: " + e.toString());
        }
    }

    public void whenDeleteDocsTaskIsDone(String result) {
        Toast.makeText(SettingsActivity.this, "Google Docs copies deleted", Toast.LENGTH_SHORT).show();
        googleDocNotes.clear();
    }

    protected static class DeleteDocsTaskParams {
        Drive driveService;
        ArrayList<Note> docNotes;

        DeleteDocsTaskParams(ArrayList<Note> docNotes, Drive driveService) {
            this.docNotes = docNotes;
            this.driveService = driveService;
        }
    }

    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    private void goToPrivacyPolicy(View v) {
        goToUrl("https://github.com/thomastriplett/CaptureNotes/blob/master/PRIVACY_POLICY.md");
    }
}
