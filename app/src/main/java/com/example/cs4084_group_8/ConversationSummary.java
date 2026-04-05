package com.example.cs4084_group_8;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class ConversationSummary {
    private String id;
    private List<String> participants;
    private Map<String, String> memberNames;
    private Map<String, String> memberProfileImageUrls;
    private String lastMessageText;
    private String lastMessageSenderUid;
    private Timestamp lastMessageAt;

    public ConversationSummary() {
        // Required for Firestore.
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public Map<String, String> getMemberNames() {
        return memberNames;
    }

    public void setMemberNames(Map<String, String> memberNames) {
        this.memberNames = memberNames;
    }

    public Map<String, String> getMemberProfileImageUrls() {
        return memberProfileImageUrls;
    }

    public void setMemberProfileImageUrls(Map<String, String> memberProfileImageUrls) {
        this.memberProfileImageUrls = memberProfileImageUrls;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public String getLastMessageSenderUid() {
        return lastMessageSenderUid;
    }

    public void setLastMessageSenderUid(String lastMessageSenderUid) {
        this.lastMessageSenderUid = lastMessageSenderUid;
    }

    public Timestamp getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Timestamp lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}
