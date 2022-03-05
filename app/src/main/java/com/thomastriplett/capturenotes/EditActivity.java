package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        EditText editText = findViewById(R.id.editText);

        Intent intent = getIntent();
        noteid = intent.getIntExtra("noteid",-1);

        Log.i("Info","noteid = "+noteid);

        if(noteid != -1) {
            Note note = NotesActivity.notes.get(noteid);
            String noteContent = note.getContent();
            editText.setText(noteContent);
        }
    }

    public void onClick(View v) {
        EditText editText = findViewById(R.id.editText);
        String content = editText.getText().toString();

        Context context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        SharedPreferences sharedPreferences = getSharedPreferences("c.sakshi.lab5", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username","");

        String title;
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String date = dateFormat.format(new Date());

        if (noteid == -1) {
            title = "NOTE_" + (NotesActivity.notes.size() + 1);
            dbHelper.saveNotes(username, title, content, date);
        } else {
            title = "NOTE_" + (noteid +1);
            dbHelper.updateNote(username, date, title, content);
        }

        Intent intent = new Intent(EditActivity.this, NotesActivity.class);
        startActivity(intent);
    }
}
