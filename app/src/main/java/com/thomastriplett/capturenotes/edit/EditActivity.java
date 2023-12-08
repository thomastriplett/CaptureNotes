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
import com.thomastriplett.capturenotes.notes.NotesActivity;
import com.thomastriplett.capturenotes.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        }


        ActionBar actionBar = getSupportActionBar();
        syncButton = actionBar.getCustomView().findViewById(R.id.sync_button);

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncNoteWithGoogleDocs();
            }
        });
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

    protected static class CreateDocTaskParams {
        Document doc;
        Docs service;

        CreateDocTaskParams(Document doc, Docs service) {
            this.doc = doc;
            this.service = service;
        }
    }

    protected static class UpdateDocTaskParams {
        Docs service;
        String docId;
        BatchUpdateDocumentRequest body;

        UpdateDocTaskParams(Docs service, String docId, BatchUpdateDocumentRequest body) {
            this.service = service;
            this.docId = docId;
            this.body = body;
        }
    }

    protected static class UpdateAndRenameDocTaskParams {
        Docs service;
        Drive driveService;
        String docId;
        BatchUpdateDocumentRequest body;
        File file;

        UpdateAndRenameDocTaskParams(Docs service, String docId, BatchUpdateDocumentRequest body, Drive driveService, File file) {
            this.service = service;
            this.docId = docId;
            this.body = body;
            this.driveService = driveService;
            this.file = file;
        }
    }

    protected static class GetDocTaskParams {
        Docs service;
        String docId;

        GetDocTaskParams(Docs service, String docId) {
            this.service = service;
            this.docId = docId;
        }
    }

    private void uploadNoteToGoogleDocs(){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Docs service created");
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Drive service created");

            if(!docId.equals("None")){
                EditActivity.GetDocTaskParams getDocParams = new EditActivity.GetDocTaskParams(docsService,docId);
                Executor executor = Executors.newSingleThreadExecutor();
                GetEditDocTaskNew getEditDocTaskNew = new GetEditDocTaskNew();
                getEditDocTaskNew.execute(getDocParams.service, getDocParams.docId, executor, new MyCallback() {
                    @Override
                    public void onComplete(Document result) {
                        // Handle the result on the main thread
                        if (result == null) {
                            Log.e("Exception", "File upload failed");
                            docId = "None";
                            uploadNoteToGoogleDocs();
                        }
                        else {
                            updateNoteInGoogleDocs(result);
                        }
                    }
                });
            }
            else {
                Document doc = new Document()
                        .setTitle(editTitle.getText().toString());
                EditActivity.CreateDocTaskParams params = new EditActivity.CreateDocTaskParams(doc, docsService);
                new CreateEditDocTask(EditActivity.this).execute(params);
            }


        } catch(IOException e) {
            Toast.makeText(EditActivity.this, "Note Not Uploaded", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
        }
        catch(GeneralSecurityException e) {
            Toast.makeText(EditActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
        }
    }

    private void syncNoteWithGoogleDocs(){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Docs service created");
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, AuthManager.getInstance().getUserCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Drive service created");

            if(!docId.equals("None")){
                EditActivity.GetDocTaskParams getDocParams = new EditActivity.GetDocTaskParams(docsService,docId);
                Executor executor = Executors.newSingleThreadExecutor();
                GetEditDocTaskNew getEditDocTaskNew = new GetEditDocTaskNew();
                getEditDocTaskNew.execute(getDocParams.service, getDocParams.docId, executor, new MyCallback() {
                    @Override
                    public void onComplete(Document result) {
                        // Handle the result on the main thread
                        if (result == null) {
                            runOnUiThread(() -> showToast("Note Not Synced, Google Doc not found"));
                        }
                        else {
                            updateNoteToMatchGoogleDoc(result);
                        }
                    }
                });
            }
            else {
                Toast.makeText(EditActivity.this, "Can't sync this note because it wasn't uploaded to Google Docs", Toast.LENGTH_SHORT).show();
            }


        } catch(IOException e) {
            Toast.makeText(EditActivity.this, "Note Not Uploaded", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
        }
        catch(GeneralSecurityException e) {
            Toast.makeText(EditActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File upload failed: " + e.toString());
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
            EditActivity.UpdateDocTaskParams updateParams = new EditActivity.UpdateDocTaskParams(docsService, docId, body);
            new UpdateEditDocTask(this).execute(updateParams);
        }
        else {
            File file = new File();
            file.set("name",editTitle.getText().toString());
            EditActivity.UpdateAndRenameDocTaskParams updateParams = new EditActivity.UpdateAndRenameDocTaskParams(docsService, docId, body, driveService, file);
            new UpdateAndRenameDocTask(this).execute(updateParams);
        }
    }

    public void updateNoteToMatchGoogleDoc(Document result) {
        String title = result.getTitle();
        String resultText = "";
        List<StructuralElement> contents = result.getBody().getContent();
        for(int i=0; i<contents.size(); i++){
            Paragraph paragraph = contents.get(i).getParagraph();
            if(paragraph != null){
                List<ParagraphElement> elements = paragraph.getElements();
                for(int j=0; j<elements.size(); j++){
                    resultText += elements.get(j).getTextRun().getContent();
                }
            }
        }
        runOnUiThread(() -> editTitle.setText(title));
        String finalResultText = resultText;
        runOnUiThread(() -> editText.setText(finalResultText));
        runOnUiThread(() -> showToast("Note Synced with Google Docs"));
    }

    public void whenCreateDocTaskIsDone(EditActivity.CreateDocTaskParams params) {
        Document doc = params.doc;
        Docs service = params.service;
        Log.d(TAG,"Created document with title: " + doc.getTitle());
        docId = doc.getDocumentId();
        Log.d(TAG,"Document ID: " + docId);

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setText(editText.getText().toString())
                .setLocation(new Location().setIndex(1))));


        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
        EditActivity.UpdateDocTaskParams updateParams = new EditActivity.UpdateDocTaskParams(service,docId,body);
        new UpdateEditDocTask(this).execute(updateParams);
    }

    public void whenUpdateDocTaskIsDone(String resultDocId) {
        Toast.makeText(EditActivity.this, "Note Saved to Google Docs", Toast.LENGTH_SHORT).show();
        saveNote(resultDocId);
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

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
        String date = dateFormat.format(new Date());

        if (noteid == -1) {
            dbHelper.saveNotes(username, title, content, date, docId);
        } else {
            dbHelper.updateNote(username, date, title, content, docId, originalTitle);
        }

        Toast.makeText(EditActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }

    private void saveNote(String docId) {
        String content = editText.getText().toString();
        String title = editTitle.getText().toString();

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
        String date = dateFormat.format(new Date());

        if (noteid == -1) {
            dbHelper.saveNotes(username, title, content, date, docId);
        } else  {
            dbHelper.updateNote(username, date, title, content, docId, originalTitle);
        }

        Toast.makeText(EditActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }

    private static class GetEditDocTaskNew {

        void execute(Docs service, String docId, Executor executor, MyCallback callback) {
            executor.execute(() -> {
                // Perform background operation here
                Document response = null;
                try {
                    response = service.documents().get(docId).execute();
                } catch (Exception e) {
                    Log.e("Exception","Error in doInBackground: "+e.toString());
                }

                // Invoke the callback on the main thread
                callback.onComplete(response);
            });
        }
    }

    private interface MyCallback {
        void onComplete(Document result);

    }
    private void showToast(String message) {
        Toast.makeText(EditActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
