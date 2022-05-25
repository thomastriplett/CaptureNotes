package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

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
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;

public class NotesActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    public static ArrayList<Note> notes = new ArrayList<>();
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    Context context;
    NoteAdapter adapter;
    String username;
    int noteCount;
    String docId;
    private RecyclerView recyclerView;
    private ImageButton addNoteButton;
    private final String TAG = "In NotesActivity";

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayOptions(getSupportActionBar().DISPLAY_SHOW_CUSTOM);
        View cView = getLayoutInflater().inflate(R.layout.activity_notes_action_bar, null);
        getSupportActionBar().setCustomView(cView);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username","");
        noteCount = sharedPreferences.getInt("noteCount",0);

        context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        notes = dbHelper.readNotes(username);

        Log.i("Info","Number of notes = "+notes.size());

        adapter = new NoteAdapter(notes, this);
        recyclerView = findViewById(R.id.notesListRecyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        addNoteButton = findViewById(R.id.sync_button);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(NotesActivity.this, EditActivity.class);
                startActivity(intent2);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(NotesActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    public void onClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Intent intent = new Intent(getApplicationContext(), EditActivity.class);
        intent.putExtra("noteid",pos);
        startActivity(intent);
    }


    @Override
    public boolean onLongClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        final Note n = notes.get(pos);
        docId = n.getDocId();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!n.getDocId().equals("None")){
                    deleteNote(n, i);
                    createGoogleDocsDeletionToast();
                }
                else {
                    deleteNote(n, i);
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Do Nothing
            }
        });

        builder.setMessage("Do you want to delete this note?");
        builder.setTitle("Delete Note");
        AlertDialog dialog =  builder.create();
        dialog.show();
        return false;
    }

    private void createGoogleDocsDeletionToast() {
        AlertDialog.Builder secondBuilder = new AlertDialog.Builder(NotesActivity.this, R.style.AlertDialogCustom);
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
        secondBuilder.setMessage("Delete copy in Google Docs too?");
        secondBuilder.setTitle("Delete Note in Google Docs");
        AlertDialog dialog =  secondBuilder.create();
        dialog.show();
    }

    private void deleteNote(Note n, int i) {
        notes.remove(n);
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);
        dbHelper.deleteNote(username,n.getTitle());

        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putInt("noteCount",noteCount-1);
        sharedPreferencesEditor.apply();
        Log.d(TAG,"Removing "+i+" item from adapter");
        adapter.notifyDataSetChanged();
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
                                .usingOAuth2(NotesActivity.this, Collections.singleton(DocsScopes.DRIVE_FILE));

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

            NotesActivity.DeleteDocTaskParams updateParams = new NotesActivity.DeleteDocTaskParams(docId, driveService);
            new DeleteDocTask(this).execute(updateParams);


        } catch(IOException e) {
            Toast.makeText(NotesActivity.this, "Google docs copy not deleted", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File deletion failed: " + e.toString());
        }
        catch(GeneralSecurityException e) {
            Toast.makeText(NotesActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File deletion failed: " + e.toString());
        }
    }

    public void whenDeleteDocTaskIsDone(String result) {
        Toast.makeText(NotesActivity.this, "Google Docs copy deleted", Toast.LENGTH_SHORT).show();
    }

    protected static class DeleteDocTaskParams {
        Drive driveService;
        String docId;

        DeleteDocTaskParams(String docId, Drive driveService) {
            this.docId = docId;
            this.driveService = driveService;
        }
    }
}
