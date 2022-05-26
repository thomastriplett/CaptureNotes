package com.thomastriplett.capturenotes;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

public class DBHelper {

    SQLiteDatabase sqLiteDatabase;

    public DBHelper(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
    }

    public void createTable() {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS notes" +
                "(id INTEGER PRIMARY KEY, username TEXT, date TEXT, docId TEXT, title TEXT, content TEXT, src TEXT)");
    }

    public ArrayList<Note> readNotes(String username) {
        createTable();
        Cursor c = sqLiteDatabase.rawQuery(String.format("SELECT * from notes where username like '%s'", username),null);

        int dateIndex = c.getColumnIndex("date");
        int docIdIndex = c.getColumnIndex("docId");
        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        c.moveToFirst();

        ArrayList<Note> notesList = new ArrayList<>();

        while (!c.isAfterLast()) {

            String title = c.getString(titleIndex);
            String date = c.getString(dateIndex);
            String docId = c.getString(docIdIndex);
            String content = c.getString(contentIndex);

            Note note = new Note(date, username, title, content, docId);
            notesList.add(note);
            c.moveToNext();
        }
        c.close();
        sqLiteDatabase.close();

        return notesList;
    }

    public void saveNotes(String username, String title, String content, String date, String docId) {
        createTable();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("date", date);
        values.put("title", title);
        values.put("content", content);
        values.put("docId", docId);
        sqLiteDatabase.insertWithOnConflict("notes", null, values, CONFLICT_REPLACE);
    }

    public void updateNote(String username, String date, String title, String content, String docId, String originalTitle) {
        createTable();
        ContentValues values = new ContentValues();
        String[] whereArgs = {originalTitle,username};
        values.put("username", username);
        values.put("date", date);
        values.put("title", title);
        values.put("content", content);
        values.put("docId", docId);
        sqLiteDatabase.updateWithOnConflict("notes", values,
                "title = ? AND username = ?", whereArgs, CONFLICT_REPLACE);
    }

    public void deleteNote(String username, String title) {
        createTable();
        String[] whereArgs = {title,username};
        sqLiteDatabase.delete("notes", "title = ? AND username = ?", whereArgs);
    }

    public void deleteNotes(String username) {
        createTable();
        String[] whereArgs = {username};
        sqLiteDatabase.delete("notes", "username = ?", whereArgs);
    }
}
