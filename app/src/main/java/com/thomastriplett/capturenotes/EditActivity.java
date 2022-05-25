package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    String docsRequest = "None";
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

        syncButton = findViewById(R.id.sync_button);

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                docsRequest = "sync";
                requestSignIn();
            }
        });
    }

    private void save() {
        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        String saveLocation = sharedPreferences.getString("saveLocation","");
        if(saveLocation.equals("googleDocs")){
            docsRequest = "save";
            requestSignIn();
        }
        else if(saveLocation.equals("appOnly")){
            saveNote();
        } else {
            Toast.makeText(this, "Error with Save Location", Toast.LENGTH_LONG).show();
        }
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
                                .usingOAuth2(EditActivity.this, Collections.singleton(DocsScopes.DRIVE_FILE));

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

    private void onCredentialReceived(GoogleAccountCredential credential){
        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Docs service created");
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Log.d(TAG,"Drive service created");

            if(docsRequest.equals("save")){
                uploadNoteToGoogleDocs();
            }
            else if(docsRequest.equals("sync")){
                syncNoteWithGoogleDocs();
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

    private void uploadNoteToGoogleDocs() {
        if(!docId.equals("None")){
            EditActivity.GetDocTaskParams getDocParams = new EditActivity.GetDocTaskParams(docsService,docId);
            new GetEditDocTask(this).execute(getDocParams);
        }
        else {
            Document doc = new Document()
                    .setTitle(editTitle.getText().toString());
            EditActivity.CreateDocTaskParams params = new EditActivity.CreateDocTaskParams(doc, docsService);
            new CreateEditDocTask(EditActivity.this).execute(params);
        }
    }

    private void syncNoteWithGoogleDocs() {
        if(!docId.equals("None")){
            EditActivity.GetDocTaskParams getDocParams = new EditActivity.GetDocTaskParams(docsService,docId);
            new GetEditDocTask(this).execute(getDocParams);
        }
        else {
            Toast.makeText(EditActivity.this, "Can't sync this note because it wasn't uploaded to Google Docs", Toast.LENGTH_SHORT).show();
        }
    }

    public void whenGetDocTaskIsDone(Document result) {
        if(docsRequest.equals("save")) {
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
        else if(docsRequest.equals("sync")) {
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
            editTitle.setText(title);
            editText.setText(resultText);
            Toast.makeText(EditActivity.this, "Note Synced with Google Docs", Toast.LENGTH_SHORT).show();
        }
    }


    public void whenGetTaskFails() {
        if(docsRequest.equals("save")){
            docId = "None";
            uploadNoteToGoogleDocs();
        }
        else if(docsRequest.equals("sync")){
            Toast.makeText(EditActivity.this, "Note Not Synced, Google Doc not found", Toast.LENGTH_SHORT).show();
        }
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

        int noteCount = sharedPreferences.getInt("noteCount",0);
        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putInt("noteCount",noteCount+1);
        sharedPreferencesEditor.apply();

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

        int noteCount = sharedPreferences.getInt("noteCount",0);
        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putInt("noteCount",noteCount+1);
        sharedPreferencesEditor.apply();

        Toast.makeText(EditActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }
}
