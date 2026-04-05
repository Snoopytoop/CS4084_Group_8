package com.example.cs4084_group_8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConversationThreadIds {
    private ConversationThreadIds() {
    }

    public static String buildConversationId(String firstUserId, String secondUserId) {
        List<String> participants = buildParticipants(firstUserId, secondUserId);
        return participants.get(0) + "_" + participants.get(1);
    }

    public static List<String> buildParticipants(String firstUserId, String secondUserId) {
        List<String> participants = new ArrayList<>();
        participants.add(firstUserId);
        participants.add(secondUserId);
        Collections.sort(participants);
        return participants;
    }
}
