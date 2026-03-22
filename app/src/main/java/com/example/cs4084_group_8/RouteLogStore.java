package com.example.cs4084_group_8;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RouteLogStore {
    private static final String PREFS_NAME = "route_log_prefs";
    private static final String KEY_ROUTE_LOGS = "route_logs_v1";

    private final SharedPreferences sharedPreferences;

    public RouteLogStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<RouteLogEntry> getAllEntries() {
        List<RouteLogEntry> entries = new ArrayList<>();
        String payload = sharedPreferences.getString(KEY_ROUTE_LOGS, "[]");
        if (payload == null || payload.trim().isEmpty()) {
            return entries;
        }

        try {
            JSONArray jsonArray = new JSONArray(payload);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                if (jsonObject == null) {
                    continue;
                }

                RouteLogEntry entry = RouteLogEntry.fromJson(jsonObject);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (Exception ignored) {
            // Keep an empty logbook if local data is invalid.
        }

        entries.sort(Comparator.comparingLong(RouteLogEntry::getLoggedAtMillis).reversed());
        return entries;
    }

    public void addEntry(RouteLogEntry entry) {
        List<RouteLogEntry> entries = getAllEntries();
        entries.add(0, entry);
        saveEntries(entries);
    }

    private void saveEntries(List<RouteLogEntry> entries) {
        JSONArray jsonArray = new JSONArray();
        for (RouteLogEntry entry : entries) {
            jsonArray.put(entry.toJson());
        }

        sharedPreferences.edit()
                .putString(KEY_ROUTE_LOGS, jsonArray.toString())
                .apply();
    }
}
