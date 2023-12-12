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

package com.thomastriplett.capturenotes.activity;

import static java.lang.Math.max;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.annotation.KeepName;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.thomastriplett.capturenotes.camera.BitmapUtils;
import com.thomastriplett.capturenotes.common.DBHelper;
import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.camera.GraphicOverlay;
import com.thomastriplett.capturenotes.google.docs.CreateGoogleDoc;
import com.thomastriplett.capturenotes.google.docs.UpdateGoogleDoc;
import com.thomastriplett.capturenotes.google.services.DocsService;
import com.thomastriplett.capturenotes.textdetector.TextRecognitionProcessor;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
  private static final int REQUEST_CAMERA_PERMISSION = 1003;
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

  private Docs docsService;
  Executor executor = Executors.newSingleThreadExecutor();
  CreateGoogleDoc createGoogleDoc = new CreateGoogleDoc();
  UpdateGoogleDoc updateGoogleDoc = new UpdateGoogleDoc();
  ActivityResultLauncher<Intent> takePictureActivityResultLauncher;
  ActivityResultLauncher<Intent> chooseImageActivityResultLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_image);
    Objects.requireNonNull(getSupportActionBar()).setIcon(R.drawable.notes);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    recordText = (TextView) findViewById(R.id.image_record_text);
    noteTitle = findViewById(R.id.image_note_title);
    noteTitle.setText(getString(R.string.default_note_title));
    findViewById(R.id.camera_button)
            .setOnClickListener(
                    view -> {
                      Log.i(TAG, "Camera button clicked");
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
                      Log.i(TAG, "Gallery button clicked");
                      startChooseImageIntentForResult();
                    });

    findViewById(R.id.image_save_button)
            .setOnClickListener(
                    view -> {
                      Log.i(TAG, "Save button clicked");
                      save();
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

    docsService = DocsService.build();

    takePictureActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK) {
                Log.d(TAG,"Capture Image result received");
                tryReloadAndDetectInImage();
            }
  });

    chooseImageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK) {
                Log.d(TAG,"Choose Image result received");
                Intent data = result.getData();
                assert data != null;
                imageUri = data.getData();
                tryReloadAndDetectInImage();
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
      saveNote(recordText.getText().toString());
    } else {
      Toast.makeText(this, "Error with Save Location", Toast.LENGTH_LONG).show();
    }
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_IMAGE_URI, imageUri);
    outState.putString(KEY_SELECTED_SIZE, selectedSize);
  }

  private void startCameraIntentForResult() {
    // Clean up last time's image
    imageUri = null;

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    try {
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.TITLE, "New Picture");
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
      imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      takePictureActivityResultLauncher.launch(takePictureIntent);
    } catch (ActivityNotFoundException e) {
      Log.e(TAG, "Take Picture Activity Not Found");
    }
  }

  private void startChooseImageIntentForResult() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    chooseImageActivityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
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
      if (selectedMode.equals(TEXT_RECOGNITION_LATIN)) {
        if (imageProcessor != null) {
          imageProcessor.stop();
        }
        imageProcessor =
                new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build(), ImageActivity.this);
      } else {
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
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
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
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
    String date = dateFormat.format(new Date());

    dbHelper.saveNotes(username, title, recording, date, docId);

    runOnUiThread(() -> Toast.makeText(ImageActivity.this, "Note Saved in App", Toast.LENGTH_SHORT).show());
  }

  private void uploadNoteToGoogleDocs(){
      Document doc = new Document()
              .setTitle(noteTitle.getText().toString());

      createGoogleDoc.execute(docsService, doc, executor, createGoogleDocResult -> {
        // Handle the result on the main thread
        if (createGoogleDocResult == null) {
          Log.e(TAG, "Doc creation failed");
          runOnUiThread(() -> Toast.makeText(ImageActivity.this, "Note Not Saved, Error Creating Google Doc", Toast.LENGTH_SHORT).show());
        }
        else {
          Log.d(TAG,"Created document with title: " + createGoogleDocResult.getTitle());
          String docId = createGoogleDocResult.getDocumentId();
          Log.d(TAG,"Document ID: " + docId);

          saveNote(recordText.getText().toString(), docId);

          List<Request> requests = new ArrayList<>();
          requests.add(new Request().setInsertText(new InsertTextRequest()
                  .setText(recordText.getText().toString())
                  .setLocation(new Location().setIndex(1))));


          BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
          updateGoogleDoc.execute(docsService, docId, body, executor, updateGoogleDocResult -> {
            // Handle the result on the main thread
            if (updateGoogleDocResult == null) {
              Log.e(TAG, "Error Adding Text to Google Doc");
              Toast.makeText(ImageActivity.this, "Note Not Saved, Error Adding Text to Google Doc", Toast.LENGTH_SHORT).show();
            }
            else {
              runOnUiThread(() -> Toast.makeText(ImageActivity.this, "Note uploaded to Google Docs", Toast.LENGTH_SHORT).show());
            }
          });
        }
      });
  }

  public void whenTextRecognitionTaskIsDone(Text text) {
    List<Text.TextBlock> textBlocks = text.getTextBlocks();
    Log.d(TAG, "There are "+textBlocks.size()+" TextBlocks");
    StringBuilder resultText = new StringBuilder();
    for(int i=0; i < textBlocks.size();i++){
      if(i != textBlocks.size()-1) {
        Text.TextBlock textBlock = textBlocks.get(i);
        resultText.append(textBlock.getText()).append(System.lineSeparator()).append(System.lineSeparator());
      } else {
        Text.TextBlock textBlock = textBlocks.get(i);
        resultText.append(textBlock.getText());
      }
    }
    recordText.setText(resultText.toString());
  }
}
