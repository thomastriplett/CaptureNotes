package com.thomastriplett.capturenotes;

import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class NoteViewHolder extends RecyclerView.ViewHolder {
    public TextView title;

    public TextView date;

    public TextView content;

    private NotesActivity notesActivity;

    public NoteViewHolder(View view) {
        super(view);
        title = (TextView) view.findViewById(R.id.notesListRowTitle);
        date = (TextView) view.findViewById(R.id.notesListRowDate);
//        content = (TextView) view.findViewById(R.id.milesAway);
    }
}
