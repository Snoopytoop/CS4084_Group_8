package com.example.cs4084_group_8;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class RouteLogSyncManager {
    public interface Callback {
        void onComplete(int uploadedCount, int failedCount);
    }

    private RouteLogSyncManager() {
    }

    public static void syncPendingEntries(
            Context context,
            FirebaseFirestore firestore,
            FirebaseUser currentUser,
            @Nullable Callback callback
    ) {
        if (context == null || firestore == null || currentUser == null) {
            if (callback != null) {
                callback.onComplete(0, 0);
            }
            return;
        }

        List<RouteLogEntry> pendingEntries = RouteLogStore.loadPendingEntries(context, currentUser.getUid());
        if (pendingEntries.isEmpty()) {
            if (callback != null) {
                callback.onComplete(0, 0);
            }
            return;
        }

        AtomicInteger remaining = new AtomicInteger(pendingEntries.size());
        AtomicInteger uploadedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (RouteLogEntry entry : pendingEntries) {
            if (TextUtils.isEmpty(entry.getId())) {
                failedCount.incrementAndGet();
                if (remaining.decrementAndGet() == 0 && callback != null) {
                    callback.onComplete(uploadedCount.get(), failedCount.get());
                }
                continue;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("authorUid", entry.getAuthorUid());
            data.put("authorName", entry.getAuthorName());
            data.put("routeName", entry.getRouteName());
            data.put("grade", entry.getGrade());
            data.put("attempts", entry.getAttempts());
            data.put("sendStatus", entry.getSendStatus());
            data.put("notes", entry.getNotes());

            long timestampMillis = entry.getSortTimestampMillis();
            if (timestampMillis > 0) {
                data.put("loggedAt", new Timestamp(new Date(timestampMillis)));
            }

            firestore.collection(FirestoreCollections.ROUTE_LOGS)
                    .document(entry.getId())
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        uploadedCount.incrementAndGet();
                        RouteLogStore.markEntrySynced(context, currentUser.getUid(), entry.getId());
                        if (remaining.decrementAndGet() == 0 && callback != null) {
                            callback.onComplete(uploadedCount.get(), failedCount.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        failedCount.incrementAndGet();
                        if (remaining.decrementAndGet() == 0 && callback != null) {
                            callback.onComplete(uploadedCount.get(), failedCount.get());
                        }
                    });
        }
    }
}
