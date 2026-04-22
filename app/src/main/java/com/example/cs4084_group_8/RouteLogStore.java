package com.example.cs4084_group_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.Timestamp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class RouteLogStore {
    private static final String PREFS_NAME = "route_log_store";
    private static final String KEY_PREFIX = "route_logs_";

    private RouteLogStore() {
    }

    public static List<RouteLogEntry> loadEntries(Context context, String ownerId) {
        List<RouteLogEntry> entries = new ArrayList<>();
        if (TextUtils.isEmpty(ownerId)) {
            return entries;
        }

        JSONArray storedEntries = readArray(context, ownerKey(ownerId));
        for (int index = 0; index < storedEntries.length(); index++) {
            JSONObject object = storedEntries.optJSONObject(index);
            if (object == null) {
                continue;
            }
            RouteLogEntry entry = fromJson(object);
            if (entry != null) {
                entries.add(entry);
            }
        }

        entries.sort(Comparator.comparing(RouteLogEntry::getSortTimestampMillis).reversed());
        return entries;
    }

    public static void saveEntry(Context context, RouteLogEntry entry) {
        if (entry == null || TextUtils.isEmpty(entry.getAuthorUid())) {
            return;
        }

        JSONArray storedEntries = readArray(context, ownerKey(entry.getAuthorUid()));
        String entryId = entry.getId();
        if (TextUtils.isEmpty(entryId)) {
            entryId = UUID.randomUUID().toString();
            entry.setId(entryId);
        }

        removeEntryFromArray(storedEntries, entryId);
        storedEntries.put(toJson(entry));
        writeArray(context, ownerKey(entry.getAuthorUid()), storedEntries);
    }

    public static boolean deleteEntry(Context context, String ownerId, String entryId) {
        if (TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(entryId)) {
            return false;
        }

        JSONArray storedEntries = readArray(context, ownerKey(ownerId));
        int originalSize = storedEntries.length();
        removeEntryFromArray(storedEntries, entryId);
        if (storedEntries.length() == originalSize) {
            return false;
        }

        writeArray(context, ownerKey(ownerId), storedEntries);
        return true;
    }

    public static List<RouteLogEntry> loadPendingEntries(Context context, String ownerId) {
        List<RouteLogEntry> pendingEntries = new ArrayList<>();
        for (RouteLogEntry entry : loadEntries(context, ownerId)) {
            if (entry.isPendingSync()) {
                pendingEntries.add(entry);
            }
        }
        return pendingEntries;
    }

    public static void markEntrySynced(Context context, String ownerId, String entryId) {
        if (TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(entryId)) {
            return;
        }

        JSONArray storedEntries = readArray(context, ownerKey(ownerId));
        for (int index = 0; index < storedEntries.length(); index++) {
            JSONObject object = storedEntries.optJSONObject(index);
            if (object == null) {
                continue;
            }
            if (entryId.equals(object.optString("id", null))) {
                try {
                    object.put("pendingSync", false);
                    object.put("syncedToServer", true);
                    storedEntries.put(index, object);
                } catch (JSONException ignored) {
                    // Value types are controlled by this class.
                }
                writeArray(context, ownerKey(ownerId), storedEntries);
                return;
            }
        }
    }

    public static RouteLogEntry createEntry(
            String authorUid,
            String authorName,
            String routeName,
            String grade,
            long attempts,
            String sendStatus,
            String notes
    ) {
        RouteLogEntry entry = new RouteLogEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setAuthorUid(authorUid);
        entry.setAuthorName(authorName);
        entry.setRouteName(routeName);
        entry.setGrade(grade);
        entry.setAttempts(attempts);
        entry.setSendStatus(sendStatus);
        entry.setNotes(notes);
        entry.setLoggedAtMillis(System.currentTimeMillis());
        entry.setLoggedAt(new Timestamp(new Date(entry.getLoggedAtMillis())));
        entry.setPendingSync(true);
        entry.setSyncedToServer(false);
        return entry;
    }

    private static RouteLogEntry fromJson(JSONObject object) {
        RouteLogEntry entry = new RouteLogEntry();
        entry.setId(object.optString("id", null));
        entry.setAuthorUid(object.optString("authorUid", null));
        entry.setAuthorName(object.optString("authorName", null));
        entry.setRouteName(object.optString("routeName", null));
        entry.setGrade(object.optString("grade", null));
        entry.setAttempts(object.optLong("attempts", 0L));
        entry.setSendStatus(object.optString("sendStatus", null));
        entry.setNotes(object.optString("notes", null));
        long loggedAtMillis = object.optLong("loggedAtMillis", 0L);
        if (loggedAtMillis > 0L) {
            entry.setLoggedAtMillis(loggedAtMillis);
            entry.setLoggedAt(new Timestamp(new Date(loggedAtMillis)));
        }
        entry.setPendingSync(object.optBoolean("pendingSync", false));
        entry.setSyncedToServer(object.optBoolean("syncedToServer", false));
        return entry;
    }

    private static JSONObject toJson(RouteLogEntry entry) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", entry.getId());
            object.put("authorUid", entry.getAuthorUid());
            object.put("authorName", entry.getAuthorName());
            object.put("routeName", entry.getRouteName());
            object.put("grade", entry.getGrade());
            object.put("attempts", entry.getAttempts());
            object.put("sendStatus", entry.getSendStatus());
            object.put("notes", entry.getNotes());
            object.put("loggedAtMillis", entry.getSortTimestampMillis());
            object.put("pendingSync", entry.isPendingSync());
            object.put("syncedToServer", entry.isSyncedToServer());
        } catch (JSONException ignored) {
            // Stored values are already validated before they reach this point.
        }
        return object;
    }

    private static void removeEntryFromArray(JSONArray entries, String entryId) {
        for (int index = entries.length() - 1; index >= 0; index--) {
            JSONObject object = entries.optJSONObject(index);
            if (object != null && entryId.equals(object.optString("id", null))) {
                entries.remove(index);
            }
        }
    }

    private static JSONArray readArray(Context context, String key) {
        String storedValue = preferences(context).getString(key, "[]");
        try {
            return new JSONArray(storedValue);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void writeArray(Context context, String key, JSONArray array) {
        preferences(context).edit()
                .putString(key, array.toString())
                .apply();
    }

    private static String ownerKey(String ownerId) {
        return KEY_PREFIX + ownerId;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}