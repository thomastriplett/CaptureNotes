package com.thomastriplett.capturenotes.common;

public class Note {

    private String date;
    private String username;
    private String title;
    private String content;
    private String docId;

    public Note(String date, String username, String title, String content, String docId) {
        this.date = date;
        this.username = username;
        this.title = title;
        this.content = content;
        this.docId = docId;
    }

    public String getDate() {return date;}

    public String getUsername() {return username;}

    public String getTitle() {return title;}

    public String getContent() {return  content;}

    public String getDocId() {return  docId;}
}
