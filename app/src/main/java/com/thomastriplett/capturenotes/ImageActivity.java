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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.annotation.KeepName;
//import com.google.mlkit.common.model.LocalModel;
//import com.google.mlkit.vision.demo.GraphicOverlay;
//import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;
//import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
//import com.google.mlkit.vision.demo.java.labeldetector.LabelDetectorProcessor;
//import com.google.mlkit.vision.demo.java.objectdetector.ObjectDetectorProcessor;
//import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor;
//import com.google.mlkit.vision.demo.java.segmenter.SegmenterProcessor;
//import com.google.mlkit.vision.demo.java.textdetector.TextRecognitionProcessor;
//import com.google.mlkit.vision.demo.preference.SettingsActivity;
//import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
//import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
//import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
//import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
//import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
//import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
//import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
//import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
//import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.thomastriplett.capturenotes.camera.GraphicOverlay;
import com.thomastriplett.capturenotes.textdetector.TextRecognitionProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class ImageActivity extends AppCompatActivity {

  private static final String TAG = "StillImageActivity";

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

  public static final String TextBlockObject = "String";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_image);

    recordText = (TextView) findViewById(R.id.image_record_text);
    noteTitle = findViewById(R.id.image_note_title);

    Intent intent = getIntent();
    recordText.setText(intent.getStringExtra("text"));
    Log.d(TAG,"String Extra = "+intent.getStringExtra("text"));

    findViewById(R.id.camera_button)
            .setOnClickListener(
                    view -> {
                      startCameraIntentForResult();
                    });

    findViewById(R.id.gallery_button)
            .setOnClickListener(
                    view -> {
                      startChooseImageIntentForResult();
                    });

    findViewById(R.id.image_save_button)
            .setOnClickListener(
                    view -> {
                      writeToFile(recordText.getText().toString());
                      saveNote(recordText.getText().toString());
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
//    preview.setImageBitmap(null);

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
      tryReloadAndDetectInImage();
    } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
      // In this case, imageUri is returned by the chooser, save it.
      imageUri = data.getData();
      tryReloadAndDetectInImage();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void tryReloadAndDetectInImage() {
    Log.d(TAG, "Try reload and detect image");
    try {
      if (imageUri == null) {
        return;
      }

      if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.
        return;
      }

      Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
      if (imageBitmap == null) {
        return;
      }

      // Clear the overlay first
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        resizedBitmap = imageBitmap;
      } else {
        // Get the dimensions of the image view
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
        graphicOverlay.setImageSourceInfo(
            resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
        imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
      } else {
        Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error retrieving saved image");
      imageUri = null;
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
              new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build());
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

  private void writeToFile(String recording) {
    try {
      String root = Environment.getExternalStorageDirectory().toString();
      File myDir = new File(root + "/CaptureNotes/SavedRecordings");
      myDir.mkdirs();

      String fname = "irecord-"+noteTitle.getText()+".txt";

      File file = new File (myDir, fname);

      FileOutputStream fos = new FileOutputStream(file);

      fos.write(noteTitle.getText().toString().getBytes());
      fos.write(System.getProperty("line.separator").getBytes());
      fos.write(System.getProperty("line.separator").getBytes());
      SimpleDateFormat sd = new SimpleDateFormat("MM/dd/yy hh:mm a");
      Date newDate = new Date();
      String date = sd.format(newDate);
      fos.write(date.getBytes());
      fos.write(System.getProperty("line.separator").getBytes());
      fos.write(System.getProperty("line.separator").getBytes());
      fos.write(recording.getBytes());
      fos.close();
      Toast.makeText(ImageActivity.this, "Note Saved to File", Toast.LENGTH_SHORT).show();
    }
    catch (IOException e) {
      Toast.makeText(ImageActivity.this, "Note Not Saved, Try Changing the Title", Toast.LENGTH_SHORT).show();
      Log.e("Exception", "File write failed: " + e.toString());
    }
  }

  private void saveNote(String recording) {

    Context context = getApplicationContext();
    sqLiteDatabase = context.openOrCreateDatabase("notes",
            Context.MODE_PRIVATE,null);
    dbHelper = new DBHelper(sqLiteDatabase);

    SharedPreferences sharedPreferences = getSharedPreferences("c.sakshi.lab5", Context.MODE_PRIVATE);
    String username = sharedPreferences.getString("username","");

    String title = noteTitle.getText().toString();
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    String date = dateFormat.format(new Date());

    dbHelper.saveNotes(username, title, recording, date);
    Toast.makeText(ImageActivity.this, "Note Saved to DB", Toast.LENGTH_SHORT).show();
  }
}
