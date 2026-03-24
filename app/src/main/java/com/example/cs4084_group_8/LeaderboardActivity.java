package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration bestListener;
    private ListenerRegistration userListener;
    private LeaderboardAdapter bestAdapter;
    private LeaderboardAdapter userAdapter;

    private RecyclerView rvBestLeaderboard;
    private RecyclerView rvUserHistory;
    private TextView tvSectionLabel;
    private Button btnGlobal;
    private Button btnPersonal;
    private EditText etSeconds;
    private EditText etMilliseconds;
    private Button btnSubmitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvBestLeaderboard = findViewById(R.id.rvBestLeaderboard);
        rvUserHistory = findViewById(R.id.rvUserHistory);
        tvSectionLabel = findViewById(R.id.tvSectionLabel);
        btnGlobal = findViewById(R.id.btnGlobal);
        btnPersonal = findViewById(R.id.btnPersonal);
        etSeconds = findViewById(R.id.etSeconds);
        etMilliseconds = findViewById(R.id.etMilliseconds);
        btnSubmitTime = findViewById(R.id.btnSubmitTime);

        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ImageButton btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ImageButton btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        ShapeableImageView ivNavProfile = findViewById(R.id.ivNavProfile);

        bestAdapter = new LeaderboardAdapter();
        rvBestLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvBestLeaderboard.setAdapter(bestAdapter);

        userAdapter = new LeaderboardAdapter();
        rvUserHistory.setLayoutManager(new LinearLayoutManager(this));
        rvUserHistory.setAdapter(userAdapter);

        btnGlobal.setOnClickListener(v -> showGlobal());
        btnPersonal.setOnClickListener(v -> showPersonal());
        showGlobal();

        btnSubmitTime.setOnClickListener(v -> submitNewTime());

        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
        });
        btnNavLeaderboard.setOnClickListener(v -> {
            // Already on leaderboard
        });
        btnNavCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
            overridePendingTransition(0, 0);
        });
        ivNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
            overridePendingTransition(0, 0);
        });

        // Adjust nav bar position for gesture/button navigation
        View bottomNav = findViewById(R.id.bottomNavCard);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = 16 + bottomInset;
            v.setLayoutParams(params);
            return insets;
        });

        loadLeaderboard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loadLeaderboard();
    }

    private void showGlobal() {
        tvSectionLabel.setText("Global best per user");
        btnGlobal.setEnabled(false);
        btnPersonal.setEnabled(true);
        rvBestLeaderboard.setVisibility(View.VISIBLE);
        rvUserHistory.setVisibility(View.GONE);
    }

    private void showPersonal() {
        tvSectionLabel.setText("Your personal history");
        btnGlobal.setEnabled(true);
        btnPersonal.setEnabled(false);
        rvBestLeaderboard.setVisibility(View.GONE);
        rvUserHistory.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bestListener != null) {
            bestListener.remove();
            bestListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    private void loadLeaderboard() {
        loadBestPerUser();
        loadUserHistory();
    }

    private void loadBestPerUser() {
        if (bestListener != null) {
            bestListener.remove();
        }
        bestListener = firestore.collection("leaderboard")
                .orderBy("totalMs", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load best leaderboard: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Map<String, LeaderboardEntry> bestByUser = new HashMap<>();
                    if (value != null) {
                        value.getDocuments().forEach(document -> {
                            LeaderboardEntry entry = document.toObject(LeaderboardEntry.class);
                            if (entry != null) {
                                entry.setId(document.getId());
                                String uid = entry.getUid();
                                if (uid == null || uid.isEmpty()) return;
                                LeaderboardEntry existing = bestByUser.get(uid);
                                if (existing == null || entry.getTotalMs() < existing.getTotalMs()) {
                                    bestByUser.put(uid, entry);
                                }
                            }
                        });
                    }

                    List<LeaderboardEntry> bestList = new ArrayList<>(bestByUser.values());
                    bestList.sort((a, b) -> Long.compare(a.getTotalMs(), b.getTotalMs()));
                    bestAdapter.setEntries(bestList);
                });
    }

    private void loadUserHistory() {
        if (userListener != null) {
            userListener.remove();
        }

        if (currentUser == null) {
            userAdapter.setEntries(new ArrayList<>());
            return;
        }

        userListener = firestore.collection("leaderboard")
                .whereEqualTo("uid", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load your history: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<LeaderboardEntry> history = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(document -> {
                            LeaderboardEntry entry = document.toObject(LeaderboardEntry.class);
                            if (entry != null) {
                                entry.setId(document.getId());
                                history.add(entry);
                            }
                        });
                    }
                    Collections.sort(history, Comparator.comparing(
                            LeaderboardEntry::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());
                    userAdapter.setEntries(history);
                });
    }

    private void submitNewTime() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to submit times.", Toast.LENGTH_SHORT).show();
            return;
        }

        String secText = etSeconds.getText().toString().trim();
        String msText = etMilliseconds.getText().toString().trim();

        if (TextUtils.isEmpty(secText)) {
            etSeconds.setError("Enter seconds");
            return;
        }
        if (TextUtils.isEmpty(msText)) {
            etMilliseconds.setError("Enter milliseconds");
            return;
        }

        long seconds;
        long milliseconds;
        try {
            seconds = Long.parseLong(secText);
            milliseconds = Long.parseLong(msText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Seconds and milliseconds must be numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (seconds < 0 || milliseconds < 0 || milliseconds >= 1000) {
            Toast.makeText(this, "Seconds must be >=0 and milliseconds must be 0-999.", Toast.LENGTH_LONG).show();
            return;
        }

        long totalMs = seconds * 1000 + milliseconds;

        final String[] usernameHolder = {""};
        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc != null && userDoc.exists()) {
                        usernameHolder[0] = userDoc.getString("username");
                    }
                    if (TextUtils.isEmpty(usernameHolder[0])) {
                        usernameHolder[0] = "Anonymous";
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", currentUser.getUid());
                    data.put("username", usernameHolder[0]);
                    data.put("seconds", seconds);
                    data.put("milliseconds", milliseconds);
                    data.put("totalMs", totalMs);
                    data.put("createdAt", FieldValue.serverTimestamp());

                    firestore.collection("leaderboard")
                            .add(data)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Time uploaded successfully.", Toast.LENGTH_SHORT).show();
                                etSeconds.setText("");
                                etMilliseconds.setText("");
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load user profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
