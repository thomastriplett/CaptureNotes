package com.thomastriplett.capturenotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class NotesActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    public static ArrayList<Note> notes = new ArrayList<>();
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    Context context;
    NoteAdapter adapter;
    String username;
    private RecyclerView recyclerView; // Layout's recyclerview
    private final String TAG = "In NotesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        getSupportActionBar().setIcon(R.drawable.notes);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        TextView myTextView = findViewById(R.id.notesListHeader);
        SharedPreferences sharedPreferences = getSharedPreferences("c.triplett.capturenotes", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username","");

        context = getApplicationContext();
        sqLiteDatabase = context.openOrCreateDatabase("notes",
                Context.MODE_PRIVATE,null);
        dbHelper = new DBHelper(sqLiteDatabase);

        notes = dbHelper.readNotes(username);

        Log.i("Info","Number of notes = "+notes.size());

        adapter = new NoteAdapter(notes, this);
        recyclerView = findViewById(R.id.notesListRecyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(NotesActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    public void onClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Intent intent = new Intent(getApplicationContext(), EditActivity.class);
        intent.putExtra("noteid",pos);
        startActivity(intent);
    }


    @Override
    public boolean onLongClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        final Note n = notes.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                notes.remove(n);
                sqLiteDatabase = context.openOrCreateDatabase("notes",
                        Context.MODE_PRIVATE,null);
                dbHelper = new DBHelper(sqLiteDatabase);
                dbHelper.deleteNote(username,n.getTitle());
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Do Nothing
            }
        });

        builder.setMessage("Do you want to delete this note?");
        builder.setTitle("Delete Note");
        AlertDialog dialog =  builder.create();
        dialog.show();
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        switch (item.getItemId()){
            case R.id.addItem:
                Intent intent2 = new Intent(NotesActivity.this, EditActivity.class);
                startActivity(intent2);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
}
