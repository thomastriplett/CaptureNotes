package com.thomastriplett.capturenotes.notes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.thomastriplett.capturenotes.common.AuthManager;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.MainActivity;
import com.thomastriplett.capturenotes.common.Note;
import com.thomastriplett.capturenotes.common.NoteAdapter;
import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.edit.EditActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Objects;

public class NotesActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    public static ArrayList<Note> notes = new ArrayList<>();
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    Context context;
    NoteAdapter adapter;
    String username;
    String docId;
    private RecyclerView recyclerView;
    private final String TAG = "In NotesActivity";

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        Objects.requireNonNull(getSupportActionBar()).setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayOptions(getSupportActionBar().DISPLAY_SHOW_CUSTOM);
        View cView = getLayoutInflater().inflate(R.layout.activity_notes_action_bar, null);
        getSupportActionBar().setCustomView(cView);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username","");

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

        ActionBar actionBar = getSupportActionBar();
        ImageButton addNoteButton = actionBar.getCustomView().findViewById(R.id.add_note_button);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Add Note Button Clicked");
                Intent intent2 = new Intent(NotesActivity.this, EditActivity.class);
                startActivity(intent2);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent mainIntent = new Intent(NotesActivity.this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Log.i(TAG, "Note in position "+pos+" clicked");
        Intent intent = new Intent(getApplicationContext(), EditActivity.class);
        intent.putExtra("noteid",pos);
        startActivity(intent);
    }


    @Override
    public boolean onLongClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Log.i(TAG, "Note in position "+pos+" long clicked");
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
                uploadNoteToGoogleDocs();
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
        Log.d(TAG,"Removing "+i+" item from adapter");
        adapter.notifyDataSetChanged();
    }

    private void uploadNoteToGoogleDocs(){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
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
