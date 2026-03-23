package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration leaderboardListener;
    private LeaderboardAdapter adapter;

    private RecyclerView rvLeaderboard;
    private EditText etSeconds;
    private EditText etMilliseconds;
    private Button btnSubmitTime;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        etSeconds = findViewById(R.id.etSeconds);
        etMilliseconds = findViewById(R.id.etMilliseconds);
        btnSubmitTime = findViewById(R.id.btnSubmitTime);
        tvEmpty = findViewById(R.id.tvEmptyLeaderboard);

        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ImageButton btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ImageButton btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        ShapeableImageView ivNavProfile = findViewById(R.id.ivNavProfile);

        adapter = new LeaderboardAdapter();
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);

        btnSubmitTime.setOnClickListener(v -> submitNewTime());

        btnNavHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        btnNavLeaderboard.setOnClickListener(v -> {
            // already on leaderboard
        });
        btnNavCreatePost.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
        ivNavProfile.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));

        loadLeaderboard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (leaderboardListener != null) {
            leaderboardListener.remove();
            leaderboardListener = null;
        }
    }

    private void loadLeaderboard() {
        if (leaderboardListener != null) {
            leaderboardListener.remove();
        }
        leaderboardListener = firestore.collection("leaderboard")
                .orderBy("totalMs", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load leaderboard: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<LeaderboardEntry> ranking = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(document -> {
                            LeaderboardEntry entry = document.toObject(LeaderboardEntry.class);
                            if (entry != null) {
                                entry.setId(document.getId());
                                ranking.add(entry);
                            }
                        });
                    }
                    adapter.setEntries(ranking);
                    tvEmpty.setVisibility(ranking.isEmpty() ? View.VISIBLE : View.GONE);
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
