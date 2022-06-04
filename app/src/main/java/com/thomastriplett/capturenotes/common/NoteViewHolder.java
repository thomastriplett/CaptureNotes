package com.thomastriplett.capturenotes.common;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.thomastriplett.capturenotes.R;
import com.thomastriplett.capturenotes.notes.NotesActivity;

public class NoteViewHolder extends RecyclerView.ViewHolder {
    public TextView title;

    public TextView date;

    public TextView content;

    private NotesActivity notesActivity;

    public NoteViewHolder(View view) {
        super(view);
        title = (TextView) view.findViewById(R.id.notesListRowTitle);
        date = (TextView) view.findViewById(R.id.notesListRowDate);
    }
}
