package com.example.cs4084_group_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

public final class OfflineSessionManager {
    private static final String PREFS_NAME = "offline_session_state";
    private static final String KEY_ENABLED = "offline_enabled";
    private static final String KEY_SESSION_UID = "offline_session_uid";
    private static final String KEY_DISPLAY_NAME = "offline_display_name";
    private static final String DEFAULT_DISPLAY_NAME = "Offline climber";

    private OfflineSessionManager() {
    }

    public static void enableOfflineMode(Context context) {
        SharedPreferences preferences = preferences(context);
        String sessionUid = preferences.getString(KEY_SESSION_UID, null);
        if (TextUtils.isEmpty(sessionUid)) {
            sessionUid = "offline-" + UUID.randomUUID();
        }

        preferences.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_SESSION_UID, sessionUid)
                .putString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
                .apply();
    }

    public static void disableOfflineMode(Context context) {
        preferences(context).edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();
    }

    public static boolean isOfflineModeEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, false);
    }

    public static SessionIdentity resolveSessionIdentity(Context context, FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
            String displayName = firebaseUser.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = firebaseUser.getEmail();
            }
            if (TextUtils.isEmpty(displayName)) {
                displayName = firebaseUser.getUid();
            }
            return new SessionIdentity(firebaseUser.getUid(), displayName, false);
        }

        if (!isOfflineModeEnabled(context)) {
            return null;
        }

        SharedPreferences preferences = preferences(context);
        String sessionUid = preferences.getString(KEY_SESSION_UID, null);
        if (TextUtils.isEmpty(sessionUid)) {
            enableOfflineMode(context);
            sessionUid = preferences(context).getString(KEY_SESSION_UID, null);
        }

        String displayName = preferences.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME);
        return new SessionIdentity(sessionUid, displayName, true);
    }

    public static String resolveDisplayName(Context context, FirebaseUser firebaseUser) {
        SessionIdentity sessionIdentity = resolveSessionIdentity(context, firebaseUser);
        if (sessionIdentity == null) {
            return "";
        }
        return sessionIdentity.getDisplayName();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static final class SessionIdentity {
        private final String uid;
        private final String displayName;
        private final boolean offline;

        SessionIdentity(String uid, String displayName, boolean offline) {
            this.uid = uid;
            this.displayName = displayName;
            this.offline = offline;
        }

        public String getUid() {
            return uid;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isOffline() {
            return offline;
        }
    }
}