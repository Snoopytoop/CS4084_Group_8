package com.example.cs4084_group_8;

import org.json.JSONObject;

public class RouteLogEntry {
    private static final String KEY_ROUTE_NAME = "routeName";
    private static final String KEY_GRADE = "grade";
    private static final String KEY_ATTEMPTS = "attempts";
    private static final String KEY_SEND_STATUS = "sendStatus";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_LOGGED_AT = "loggedAt";

    private final String routeName;
    private final String grade;
    private final int attempts;
    private final String sendStatus;
    private final String notes;
    private final long loggedAtMillis;

    public RouteLogEntry(
            String routeName,
            String grade,
            int attempts,
            String sendStatus,
            String notes,
            long loggedAtMillis
    ) {
        this.routeName = routeName;
        this.grade = grade;
        this.attempts = attempts;
        this.sendStatus = sendStatus;
        this.notes = notes;
        this.loggedAtMillis = loggedAtMillis;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getGrade() {
        return grade;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public String getNotes() {
        return notes;
    }

    public long getLoggedAtMillis() {
        return loggedAtMillis;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_ROUTE_NAME, routeName);
            jsonObject.put(KEY_GRADE, grade);
            jsonObject.put(KEY_ATTEMPTS, attempts);
            jsonObject.put(KEY_SEND_STATUS, sendStatus);
            jsonObject.put(KEY_NOTES, notes);
            jsonObject.put(KEY_LOGGED_AT, loggedAtMillis);
        } catch (Exception ignored) {
            // Keep best-effort local persistence.
        }
        return jsonObject;
    }

    public static RouteLogEntry fromJson(JSONObject jsonObject) {
        String routeName = jsonObject.optString(KEY_ROUTE_NAME, "").trim();
        if (routeName.isEmpty()) {
            return null;
        }

        int attempts = jsonObject.optInt(KEY_ATTEMPTS, 0);
        if (attempts <= 0) {
            return null;
        }

        String grade = jsonObject.optString(KEY_GRADE, "");
        String sendStatus = jsonObject.optString(KEY_SEND_STATUS, "");
        String notes = jsonObject.optString(KEY_NOTES, "");
        long loggedAtMillis = jsonObject.optLong(KEY_LOGGED_AT, System.currentTimeMillis());
        return new RouteLogEntry(routeName, grade, attempts, sendStatus, notes, loggedAtMillis);
    }
}
