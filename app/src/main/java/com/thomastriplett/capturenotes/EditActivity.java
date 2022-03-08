package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditActivity extends AppCompatActivity {

    int noteid = -1;
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    String originalTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        EditText editText = findViewById(R.id.editText);
        EditText editTitle = findViewById(R.id.editTitle);

        Intent intent = getIntent();
        noteid = intent.getIntExtra("noteid",-1);

        Log.i("Info","noteid = "+noteid);

        if(noteid != -1) {
            Note note = NotesActivity.notes.get(noteid);
            editText.setText(note.getContent());
            editTitle.setText(note.getTitle());
            originalTitle = note.getTitle();
        }
    }

    public void onClick(View v) {
        EditText editText = findViewById(R.id.editText);
        EditText editTitle = findViewById(R.id.editTitle);
        String content = editText.getText().toString();
        String title = editTitle.getText().toString();

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.sakshi.lab5", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String date = dateFormat.format(new Date());

        if (noteid == -1) {
            dbHelper.saveNotes(username, title, content, date);
        } else {
            dbHelper.updateNote(username, date, title, content, originalTitle);
        }

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }
}
