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

package com.thomastriplett.capturenotes.textdetector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.common.InputImage;
import com.thomastriplett.capturenotes.ImageActivity;
import com.thomastriplett.capturenotes.StillImageActivity;
import com.thomastriplett.capturenotes.camera.GraphicOverlay;
import com.thomastriplett.capturenotes.VisionProcessorBase;
//import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.Text.Element;
import com.google.mlkit.vision.text.Text.Line;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import java.util.List;

/** Processor for the text detector demo. */
public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

  private static final String TAG = "TextRecProcessor";

  private final TextRecognizer textRecognizer;
  private final Boolean shouldGroupRecognizedTextInBlocks;
  private final Boolean showLanguageTag;

  Context context;
  private static final int RC_OCR_CAPTURE = 9003;

  public TextRecognitionProcessor(
      Context context, TextRecognizerOptionsInterface textRecognizerOptions) {
    super(context);
    shouldGroupRecognizedTextInBlocks = true;
    showLanguageTag = false;
    textRecognizer = TextRecognition.getClient(textRecognizerOptions);
    this.context = context;
  }

  @Override
  public void stop() {
    super.stop();
    textRecognizer.close();
  }

  @Override
  protected Task<Text> detectInImage(InputImage image) {
    return textRecognizer.process(image);
  }

  @Override
  protected void onSuccess(@NonNull Text text, @NonNull GraphicOverlay graphicOverlay) {
    Log.d(TAG, "On-device Text detection successful");
    logExtrasForTesting(text);
//    graphicOverlay.addText(text.getText());
//    graphicOverlay.add(
//        new TextGraphic(graphicOverlay, text, shouldGroupRecognizedTextInBlocks, showLanguageTag));
    Intent data = new Intent(context,StillImageActivity.class);
    data.putExtra("text", text.getText());
    context.startActivity(data);
  }

  private static void logExtrasForTesting(Text text) {
    if (text != null) {
      Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");
      for (int i = 0; i < text.getTextBlocks().size(); ++i) {
        List<Line> lines = text.getTextBlocks().get(i).getLines();
        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size()));
        for (int j = 0; j < lines.size(); ++j) {
          List<Element> elements = lines.get(j).getElements();
          Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size()));
          for (int k = 0; k < elements.size(); ++k) {
            Element element = elements.get(k);
            Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.getText()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Detected text element %d has a bounding box: %s",
                    k, element.getBoundingBox().flattenToString()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Expected corner point size is 4, get %d", element.getCornerPoints().length));
            for (Point point : element.getCornerPoints()) {
              Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                      "Corner point for element %d is located at: x - %d, y = %d",
                      k, point.x, point.y));
            }
          }
        }
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.w(TAG, "Text detection failed." + e);
  }
}
