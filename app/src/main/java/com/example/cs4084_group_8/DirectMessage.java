package com.example.cs4084_group_8;

import com.google.firebase.Timestamp;

public class DirectMessage {
    private String id;
    private String senderUid;
    private String senderName;
    private String text;
    private Timestamp createdAt;
    private boolean pendingWrite;

    public DirectMessage() {
        // Required for Firestore.
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPendingWrite() {
        return pendingWrite;
    }

    public void setPendingWrite(boolean pendingWrite) {
        this.pendingWrite = pendingWrite;
    }
}
