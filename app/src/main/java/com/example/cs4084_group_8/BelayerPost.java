package com.example.cs4084_group_8;

import com.google.firebase.Timestamp;

public class BelayerPost {
    private String id;
    private String authorUid;
    private String authorName;
    private String wallName;
    private String climbDays;
    private String climbTimes;
    private String belayCapability;
    private String climbCapability;
    private String contactHandle;
    private String notes;
    private Timestamp createdAt;

    public BelayerPost() {
        // Needed for Firestore deserialization.
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getWallName() {
        return wallName;
    }

    public void setWallName(String wallName) {
        this.wallName = wallName;
    }

    public String getClimbDays() {
        return climbDays;
    }

    public void setClimbDays(String climbDays) {
        this.climbDays = climbDays;
    }

    public String getClimbTimes() {
        return climbTimes;
    }

    public void setClimbTimes(String climbTimes) {
        this.climbTimes = climbTimes;
    }

    public String getBelayCapability() {
        return belayCapability;
    }

    public void setBelayCapability(String belayCapability) {
        this.belayCapability = belayCapability;
    }

    public String getClimbCapability() {
        return climbCapability;
    }

    public void setClimbCapability(String climbCapability) {
        this.climbCapability = climbCapability;
    }

    public String getContactHandle() {
        return contactHandle;
    }

    public void setContactHandle(String contactHandle) {
        this.contactHandle = contactHandle;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
