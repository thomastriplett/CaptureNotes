package com.thomastriplett.capturenotes;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class DBHelper {

    SQLiteDatabase sqLiteDatabase;

    public DBHelper(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
    }

    public void createTable() {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS notes" +
                "(id INTEGER PRIMARY KEY, username TEXT, date TEXT, title TEXT, content TEXT, src TEXT)");
    }

    public ArrayList<Note> readNotes(String username) {
        createTable();
        Cursor c = sqLiteDatabase.rawQuery(String.format("SELECT * from notes where username like '%s'", username),null);

        int dateIndex = c.getColumnIndex("date");
        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        c.moveToFirst();

        ArrayList<Note> notesList = new ArrayList<>();

        while (!c.isAfterLast()) {

            String title = c.getString(titleIndex);
            String date = c.getString(dateIndex);
            String content = c.getString(contentIndex);

            Note note = new Note(date, username, title, content);
            notesList.add(note);
            c.moveToNext();
        }
        c.close();
        sqLiteDatabase.close();

        return notesList;
    }

    public void saveNotes(String username, String title, String content, String date) {
        createTable();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("date", date);
        values.put("title", title);
        values.put("content", content);
        sqLiteDatabase.insertWithOnConflict("notes", null, values, CONFLICT_REPLACE);
    }

    public void updateNote(String username, String date, String title, String content) {
        createTable();
//        sqLiteDatabase.execSQL(String.format("UPDATE notes set content = '%s', date = '%s' where title = '%s' AND username = '%s'",
//                content, date, title, username));
        ContentValues values = new ContentValues();
        String[] whereArgs = {title,username};
        values.put("username", username);
        values.put("date", date);
        values.put("title", title);
        values.put("content", content);
        sqLiteDatabase.updateWithOnConflict("notes", values,
                "title = ? AND username = ?", whereArgs, CONFLICT_REPLACE);
    }

    public void deleteNote(String username, String title) {
        createTable();
        String[] whereArgs = {title,username};
        sqLiteDatabase.delete("notes", "title = ? AND username = ?", whereArgs);
    }
}
