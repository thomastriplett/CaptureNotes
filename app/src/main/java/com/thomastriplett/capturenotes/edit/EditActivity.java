package com.thomastriplett.capturenotes.edit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.DeleteContentRangeRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Paragraph;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.thomastriplett.capturenotes.common.AuthManager;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.common.Note;
import com.thomastriplett.capturenotes.google.docs.CreateGoogleDoc;
import com.thomastriplett.capturenotes.google.docs.GetGoogleDoc;
import com.thomastriplett.capturenotes.google.docs.UpdateGoogleDoc;
import com.thomastriplett.capturenotes.google.services.DocsService;
import com.thomastriplett.capturenotes.google.services.DriveService;
import com.thomastriplett.capturenotes.notes.NotesActivity;
import com.thomastriplett.capturenotes.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EditActivity extends AppCompatActivity {

    int noteid = -1;
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    String originalTitle;
    String docId = "None";
    private TextView editText;
    private TextView editTitle;
    private static final String TAG = "EditActivity";
    private Docs docsService;
    private Drive driveService;
    private ImageButton syncButton;

    /** Application name. */
    private static final String APPLICATION_NAME = "CaptureNotes";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    Executor executor = Executors.newSingleThreadExecutor();
    GetGoogleDoc getGoogleDoc = new GetGoogleDoc();
    CreateGoogleDoc createGoogleDoc = new CreateGoogleDoc();
    UpdateGoogleDoc updateGoogleDoc = new UpdateGoogleDoc();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayOptions(getSupportActionBar().DISPLAY_SHOW_CUSTOM);
        View cView = getLayoutInflater().inflate(R.layout.activity_edit_action_bar, null);
        getSupportActionBar().setCustomView(cView);

        editText = (TextView) findViewById(R.id.editText);
        editTitle = (TextView) findViewById(R.id.editTitle);

        findViewById(R.id.edit_save_button)
                .setOnClickListener(
                        view -> {
                            Log.d(TAG, "Save button clicked");
                            save();
                        });

        Intent intent = getIntent();
        noteid = intent.getIntExtra("noteid",-1);

        Log.i("Info","noteid = "+noteid);

        if(noteid != -1) {
            Note note = NotesActivity.notes.get(noteid);
            editText.setText(note.getContent());
            editTitle.setText(note.getTitle());
            originalTitle = note.getTitle();
            docId = note.getDocId();
        } else {
            Log.e(TAG, "Note ID was not sent by NotesActivity");
            showToast("Something went wrong");
        }


        ActionBar actionBar = getSupportActionBar();
        syncButton = actionBar.getCustomView().findViewById(R.id.sync_button);

        docsService = DocsService.build();
        driveService = DriveService.build();

        syncButton.setOnClickListener(v -> syncNoteWithGoogleDocs());
    }

    private void save() {
        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");
        if(saveLocation.equals("googleDocs")){
            uploadNoteToGoogleDocs();
        }
        else if(saveLocation.equals("appOnly")){
            saveNote();
        } else {
            Toast.makeText(this, "Error with Save Location", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadNoteToGoogleDocs(){
        if(!docId.equals("None")){
            getGoogleDoc.execute(docsService, docId, executor, result -> {
                // Handle the result on the main thread
                if (result == null) {
                    Log.e(TAG, "Google Doc not found");
                    docId = "None";
                    uploadNoteToGoogleDocs();
                }
                else {
                    updateNoteInGoogleDocs(result);
                }
            });
        }
        else {
            Document doc = new Document()
                    .setTitle(editTitle.getText().toString());
            createGoogleDoc.execute(docsService, doc, executor, result -> {
                // Handle the result on the main thread
                if (result == null) {
                    Log.e("Exception", "File upload failed");
                }
                else {
                    Log.d(TAG,"Created document with title: " + result.getTitle());
                    docId = result.getDocumentId();
                    Log.d(TAG,"Document ID: " + docId);

                    List<Request> requests = new ArrayList<>();
                    requests.add(new Request().setInsertText(new InsertTextRequest()
                            .setText(editText.getText().toString())
                            .setLocation(new Location().setIndex(1))));


                    BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
                    updateGoogleDoc.execute(docsService, docId, body, executor, updateGoogleDocResult -> {
                        // Handle the result on the main thread
                        if (result == null) {
                            Log.e("Exception", "File upload failed");
                            runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show());
                        }
                        else {
                            runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Saved to Google Docs", Toast.LENGTH_SHORT).show());
                            saveNote();
                        }
                    });
                }
            });
        }
    }

    private void syncNoteWithGoogleDocs(){
        if(!docId.equals("None")){
            getGoogleDoc.execute(docsService, docId, executor, result -> {
                // Handle the result on the main thread
                if (result == null) {
                    runOnUiThread(() -> showToast("Note Not Synced, Google Doc not found"));
                }
                else {
                    updateNoteToMatchGoogleDoc(result);
                }
            });
        }
        else {
            Toast.makeText(EditActivity.this, "Can't sync this note because it wasn't uploaded to Google Docs", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateNoteInGoogleDocs(Document result) {
        List<Request> requests = new ArrayList<>();
        List<StructuralElement> contents = result.getBody().getContent();
        int endIndex = 0;
        for (int i = 0; i < contents.size(); i++) {
            StructuralElement element = contents.get(i);
            int elementSize;
            if (element.getStartIndex() == null) {
                elementSize = element.getEndIndex();
            } else {
                elementSize = element.getEndIndex() - element.getStartIndex();
            }
            endIndex += elementSize;
        }

        requests.add(new Request().setDeleteContentRange(
                new DeleteContentRangeRequest()
                        .setRange(new Range()
                                .setStartIndex(1)
                                .setEndIndex(endIndex - 1))
        ));
        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setText(editText.getText().toString())
                .setLocation(new Location().setIndex(1))));
        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);

        if(editTitle.getText().toString().equals(originalTitle)) {
            updateGoogleDoc.execute(docsService, docId, body, executor, updateGoogleDocResult -> {
                // Handle the result on the main thread
                if (result == null) {
                    Log.e("Exception", "File upload failed");
                    runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show());
                }
                else {
                    runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Saved to Google Docs", Toast.LENGTH_SHORT).show());
                    saveNote();
                }
            });
        }
        else {
            File file = new File();
            file.set("name",editTitle.getText().toString());
            updateGoogleDoc.execute(docsService, docId, body, driveService, file, executor, updateGoogleDocResult -> {
                // Handle the result on the main thread
                if (result == null) {
                    Log.e("Exception", "File upload failed");
                    runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show());
                }
                else {
                    runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Saved to Google Docs", Toast.LENGTH_SHORT).show());
                    saveNote();
                }
            });
        }
    }

    public void updateNoteToMatchGoogleDoc(Document result) {
        String title = result.getTitle();
        StringBuilder resultText = new StringBuilder();
        List<StructuralElement> contents = result.getBody().getContent();
        for(int i=0; i<contents.size(); i++){
            Paragraph paragraph = contents.get(i).getParagraph();
            if(paragraph != null){
                List<ParagraphElement> elements = paragraph.getElements();
                for(int j=0; j<elements.size(); j++){
                    resultText.append(elements.get(j).getTextRun().getContent());
                }
            }
        }
        runOnUiThread(() -> editTitle.setText(title));
        String finalResultText = resultText.toString();
        runOnUiThread(() -> editText.setText(finalResultText));
        runOnUiThread(() -> showToast("Note Synced with Google Docs"));
    }

    private void saveNote() {
        String content = editText.getText().toString();
        String title = editTitle.getText().toString();

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
        String date = dateFormat.format(new Date());

        if (noteid == -1) {
            dbHelper.saveNotes(username, title, content, date, docId);
        } else {
            dbHelper.updateNote(username, date, title, content, docId, originalTitle);
        }

        runOnUiThread(() -> Toast.makeText(EditActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show());

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(EditActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
