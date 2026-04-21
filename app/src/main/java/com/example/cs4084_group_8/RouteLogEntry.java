package com.example.cs4084_group_8;

import com.google.firebase.Timestamp;

public class RouteLogEntry {
    private String id;
    private String authorUid;
    private String authorName;
    private String routeName;
    private String grade;
    private long attempts;
    private String sendStatus;
    private String notes;
    private Timestamp loggedAt;
    private Long loggedAtMillis;

    public RouteLogEntry() {
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

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public long getAttempts() {
        return attempts;
    }

    public void setAttempts(long attempts) {
        this.attempts = attempts;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Timestamp loggedAt) {
        this.loggedAt = loggedAt;
        if (loggedAt != null) {
            this.loggedAtMillis = loggedAt.toDate().getTime();
        }
    }

    public Long getLoggedAtMillis() {
        return loggedAtMillis;
    }

    public void setLoggedAtMillis(Long loggedAtMillis) {
        this.loggedAtMillis = loggedAtMillis;
        if (loggedAtMillis != null && loggedAt == null) {
            this.loggedAt = new Timestamp(new java.util.Date(loggedAtMillis));
        }
    }

    public long getSortTimestampMillis() {
        if (loggedAt != null) {
            return loggedAt.toDate().getTime();
        }
        return loggedAtMillis == null ? 0L : loggedAtMillis;
    }
}
