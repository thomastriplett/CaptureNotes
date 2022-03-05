package com.thomastriplett.capturenotes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class ImageActivity extends AppCompatActivity {

    private TextView cameraInstr;
    private TextView recordText;
    private TextView noteTitle;

    private ImageView newButton;
    private ImageView saveButton;

    private static final int RC_OCR_CAPTURE = 9003;
    private final String TAG = "In ImageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        cameraInstr = (TextView) findViewById(R.id.camera_instr);
        recordText = (TextView) findViewById(R.id.record_text);
        noteTitle = findViewById(R.id.note_title);

        newButton = (ImageView) findViewById(R.id.new_button);
        saveButton = findViewById(R.id.save_button);

        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ImageActivity.this, OcrCaptureActivity.class);
                startActivityForResult(intent, RC_OCR_CAPTURE);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToFile(recordText.getText().toString());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    String text = data.getStringExtra(OcrCaptureActivity.TextBlockObject);
                    Log.d(TAG,"Text Read Successfully");

                    Random rand = new Random();
                    int n = rand.nextInt();
                    noteTitle.setText("img"+n);
                    if(recordText.getText().toString().equals("")){
                        recordText.setText(text);
                    }
                    else {
                        String currentText = recordText.getText().toString();
                        recordText.setText(currentText+" "+text);
                    }
                    Log.d(TAG, "Text read: " + text);
                } else {
                    Log.d(TAG, "No Text captured, intent data is null");
                }
            } else {
                Log.d(TAG,String.format(getString(R.string.ocr_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
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
            Toast.makeText(ImageActivity.this, "Note Saved", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Toast.makeText(ImageActivity.this, "Note Not Saved, Try Changing the Title", Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
