package com.thomastriplett.capturenotes.common;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.notes.NotesActivity;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteViewHolder> {

    private static final String TAG = "NeighborAdapter";
    private List<Note> notesList;
    private NotesActivity notesActivity;

    public NoteAdapter(List<Note> notesList, NotesActivity na) {
        this.notesList = notesList;
        notesActivity = na;
    }

    @Override
    public NoteViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: MAKING NEW");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notes_list_row, parent, false);
        itemView.setOnClickListener(notesActivity);
        itemView.setOnLongClickListener(notesActivity);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = notesList.get(position);
        holder.title.setText(note.getTitle());
        holder.date.setText(new StringBuilder()
                .append(notesActivity.getString(R.string.notes_row_date_header))
                .append(" ")
                .append(note.getDate()).toString());
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }
}
