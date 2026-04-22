package com.example.cs4084_group_8;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class ServerFeatureGate {
    private ServerFeatureGate() {
    }

    public static boolean isServerFeatureBlocked(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean forcedOfflineSession = OfflineSessionManager.isOfflineModeEnabled(context) && user == null;
        return forcedOfflineSession || !NetworkStatus.isOnline(context);
    }

    public static boolean ensureServerFeatureAvailable(Context context) {
        if (isServerFeatureBlocked(context)) {
            Toast.makeText(context, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean canUseMessaging(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, R.string.chat_requires_login_offline, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
