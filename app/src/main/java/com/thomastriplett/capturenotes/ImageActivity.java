/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thomastriplett.capturenotes;

import static java.lang.Math.max;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.annotation.KeepName;
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
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.drive.DriveScopes;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.thomastriplett.capturenotes.camera.GraphicOverlay;
import com.thomastriplett.capturenotes.textdetector.TextRecognitionProcessor;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class ImageActivity extends AppCompatActivity {

  private static final String TAG = "ImageActivity";

  private static final String OBJECT_DETECTION = "Object Detection";

  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";

  private static final String SIZE_SCREEN = "w:screen"; // Match screen width
  private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
  private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
  private static final String SIZE_ORIGINAL = "w:original"; // Original image size

  private static final String KEY_IMAGE_URI = "com.google.mlkit.vision.demo.KEY_IMAGE_URI";
  private static final String KEY_SELECTED_SIZE = "com.google.mlkit.vision.demo.KEY_SELECTED_SIZE";

  private static final int REQUEST_IMAGE_CAPTURE = 1001;
  private static final int REQUEST_CHOOSE_IMAGE = 1002;
  private static final int REQUEST_CAMERA_PERMISSION = 1003;
  private static final int REQUEST_STORAGE_PERMISSION = 1004;

  private GraphicOverlay graphicOverlay;
  private String selectedMode = OBJECT_DETECTION;
  private String selectedSize = SIZE_SCREEN;

  boolean isLandScape;

  private Uri imageUri;
  private int imageMaxWidth;
  private int imageMaxHeight;
  private TextRecognitionProcessor imageProcessor;

  private TextView recordText;
  private TextView noteTitle;

  DBHelper dbHelper;
  SQLiteDatabase sqLiteDatabase;

  /** Application name. */
  private static final String APPLICATION_NAME = "CaptureNotes";
  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_image);
    getSupportActionBar().setIcon(R.drawable.notes);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    recordText = (TextView) findViewById(R.id.image_record_text);
    noteTitle = findViewById(R.id.image_note_title);
    noteTitle.setText(getString(R.string.default_note_title));
    findViewById(R.id.camera_button)
            .setOnClickListener(
                    view -> {
                      Log.d(TAG, "Camera button clicked");
                      if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                              == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                      } else {
                        startCameraIntentForResult();
                      }
                    });

    findViewById(R.id.gallery_button)
            .setOnClickListener(
                    view -> {
                      Log.d(TAG, "Gallery button clicked");
                      startChooseImageIntentForResult();
                    });

    ImageView saveButton = (ImageView) findViewById(R.id.image_save_button);
    registerForContextMenu(saveButton);

    findViewById(R.id.image_save_button)
            .setOnClickListener(
                    view -> {
                      Log.d(TAG, "Save button clicked");
                      view.showContextMenu();
                    });

    graphicOverlay = findViewById(R.id.graphic_overlay);

    selectedMode = TEXT_RECOGNITION_LATIN;
    createImageProcessor();
    tryReloadAndDetectInImage();

    isLandScape =
        (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

    if (savedInstanceState != null) {
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
      selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
    }

    View rootView = findViewById(R.id.root);
    rootView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                imageMaxWidth = rootView.getWidth();
                imageMaxHeight = rootView.getHeight();
                if (SIZE_SCREEN.equals(selectedSize)) {
                  tryReloadAndDetectInImage();
                }
              }
            });
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    if (v.getId()==R.id.image_save_button) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.save_button_context_menu, menu);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.R)
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
      case R.id.saveInGoogleDocsItem:
        requestSignIn();
        return true;
      case R.id.saveOnlyInAppItem:
        saveNote(recordText.getText().toString());
        return true;
      default:
        return super.onContextItemSelected(item);
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
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createImageProcessor();
    tryReloadAndDetectInImage();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  @Override
  public void onBackPressed() {
    Intent mainIntent = new Intent(ImageActivity.this, MainActivity.class);
    startActivity(mainIntent);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_IMAGE_URI, imageUri);
    outState.putString(KEY_SELECTED_SIZE, selectedSize);
  }

  private void startCameraIntentForResult() {
    // Clean up last time's image
    imageUri = null;

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.TITLE, "New Picture");
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
      imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  private void startChooseImageIntentForResult() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      Log.d(TAG,"Capture Image result received");
      tryReloadAndDetectInImage();
    } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
      // In this case, imageUri is returned by the chooser, save it.
      Log.d(TAG,"Choose Image result received");
      imageUri = data.getData();
      tryReloadAndDetectInImage();
    } else if (requestCode == 400 && resultCode == RESULT_OK) {
      Log.d(TAG,"Sign in result received");
      handleSignInIntent(data);
    } else {
      Log.d(TAG,"Unknown result received, requestCode = "+requestCode);
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startCameraIntentForResult();
      } else {
        Toast.makeText(this, "Can't use camera without camera permission", Toast.LENGTH_LONG).show();
      }
    }
  }

  private void handleSignInIntent(Intent data) {
    GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
              @Override
              public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                GoogleAccountCredential credential = GoogleAccountCredential
                        .usingOAuth2(ImageActivity.this,Collections.singleton(DocsScopes.DRIVE_FILE));

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

  private void tryReloadAndDetectInImage() {
    Log.d(TAG, "Try reload and detect image");
    try {
      if (imageUri == null) {
        Log.d(TAG,"imageUri is null");
        return;
      }

      if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.
        Log.d(TAG,"UI layout has not finished yet, will reload once it's ready");
        return;
      }
      Log.d(TAG,"Getting the imageBitmap");
      Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
      if (imageBitmap == null) {
        Log.d(TAG,"imageBitmap is null");
        return;
      }

      // Clear the overlay first
      Log.d(TAG,"Clearing the graphic overlay");
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        Log.d(TAG,"Bitmap is original size");
        resizedBitmap = imageBitmap;
      } else {
        // Get the dimensions of the image view
        Log.d(TAG,"Resizing bitmap");
        Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

        // Determine how much to scale down the image
        float scaleFactor =
            max(
                (float) imageBitmap.getWidth() / (float) targetedSize.first,
                (float) imageBitmap.getHeight() / (float) targetedSize.second);

        resizedBitmap =
            Bitmap.createScaledBitmap(
                imageBitmap,
                (int) (imageBitmap.getWidth() / scaleFactor),
                (int) (imageBitmap.getHeight() / scaleFactor),
                true);
      }

      if (imageProcessor != null) {
        Log.d(TAG,"imageProcessor is not null");
        graphicOverlay.setImageSourceInfo(
            resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
        imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
      } else {
        Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error retrieving saved image");
    }
  }

  private Pair<Integer, Integer> getTargetedWidthHeight() {
    int targetWidth;
    int targetHeight;

    switch (selectedSize) {
      case SIZE_SCREEN:
        targetWidth = imageMaxWidth;
        targetHeight = imageMaxHeight;
        break;
      case SIZE_640_480:
        targetWidth = isLandScape ? 640 : 480;
        targetHeight = isLandScape ? 480 : 640;
        break;
      case SIZE_1024_768:
        targetWidth = isLandScape ? 1024 : 768;
        targetHeight = isLandScape ? 768 : 1024;
        break;
      default:
        throw new IllegalStateException("Unknown size");
    }

    return new Pair<>(targetWidth, targetHeight);
  }

  private void createImageProcessor() {
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
    try {
      switch (selectedMode) {
        case TEXT_RECOGNITION_LATIN:
          if (imageProcessor != null) {
            imageProcessor.stop();
          }
          imageProcessor =
              new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build(), ImageActivity.this);
          break;
        default:
          Log.e(TAG, "Unknown selectedMode: " + selectedMode);
      }
    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor: " + selectedMode, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  private void saveNote(String recording) {

    Context context = getApplicationContext();
    sqLiteDatabase = context.openOrCreateDatabase("notes",
            Context.MODE_PRIVATE,null);
    dbHelper = new DBHelper(sqLiteDatabase);

    SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
    String username = sharedPreferences.getString("username","");

    String title = noteTitle.getText().toString();
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
    String date = dateFormat.format(new Date());

    dbHelper.saveNotes(username, title, recording, date, "None");
    Toast.makeText(ImageActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show();
  }

  private void saveNote(String recording, String docId) {

    Context context = getApplicationContext();
    sqLiteDatabase = context.openOrCreateDatabase("notes",
            Context.MODE_PRIVATE,null);
    dbHelper = new DBHelper(sqLiteDatabase);

    SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
    String username = sharedPreferences.getString("username","");

    String title = noteTitle.getText().toString();
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
    String date = dateFormat.format(new Date());

    dbHelper.saveNotes(username, title, recording, date, docId);
    Toast.makeText(ImageActivity.this, "Note Saved to DB", Toast.LENGTH_SHORT).show();
  }

  private void onCredentialReceived(GoogleAccountCredential credential){
    try{
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

      Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
              .setApplicationName(APPLICATION_NAME)
              .build();

      Document doc = new Document()
              .setTitle(noteTitle.getText().toString());

      CreateDocTaskParams params = new CreateDocTaskParams(doc, service);
      new CreateDocTask(ImageActivity.this).execute(params);

    } catch(IOException e) {
      Toast.makeText(ImageActivity.this, "Note Not Uploaded", Toast.LENGTH_SHORT).show();
      Log.e("Exception", "File upload failed: " + e.toString());
    }
    catch(GeneralSecurityException e) {
      Toast.makeText(ImageActivity.this, "Security Issue", Toast.LENGTH_SHORT).show();
      Log.e("Exception", "File upload failed: " + e.toString());
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

  public void whenCreateDocTaskIsDone(CreateDocTaskParams params) {
    Document doc = params.doc;
    Docs service = params.service;
    Log.d(TAG,"Created document with title: " + doc.getTitle());
    String docId = doc.getDocumentId();
    Log.d(TAG,"Document ID: " + docId);

    saveNote(recordText.getText().toString(), docId);

    List<Request> requests = new ArrayList<>();
    requests.add(new Request().setInsertText(new InsertTextRequest()
            .setText(recordText.getText().toString())
            .setLocation(new Location().setIndex(1))));


    BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
    UpdateDocTaskParams updateParams = new UpdateDocTaskParams(service,docId,body);
    new UpdateDocTask(this).execute(updateParams);
  }

  public void whenUpdateDocTaskIsDone(BatchUpdateDocumentResponse result) {
    Toast.makeText(ImageActivity.this, "Note Saved to Google Docs", Toast.LENGTH_SHORT).show();
  }

  public void whenTextRecognitionTaskIsDone(Text text) {
    List<Text.TextBlock> textBlocks = text.getTextBlocks();
    Log.d(TAG, "There are "+textBlocks.size()+" TextBlocks");
    String resultText = "";
    for(int i=0; i < textBlocks.size();i++){
      if(i != textBlocks.size()-1) {
        Text.TextBlock textBlock = textBlocks.get(i);
        resultText = resultText + textBlock.getText() + System.lineSeparator() + System.lineSeparator();
      } else {
        Text.TextBlock textBlock = textBlocks.get(i);
        resultText = resultText + textBlock.getText();
      }
    }
    recordText.setText(resultText);
  }
}
