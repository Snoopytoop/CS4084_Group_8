package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FindBelayerActivity extends AppCompatActivity {
    private TextInputLayout tilBelayerName;
    private TextInputLayout tilBelayerWall;
    private TextInputLayout tilBelayerDays;
    private TextInputLayout tilBelayerTimes;
    private TextInputLayout tilBelayerCapability;
    private TextInputLayout tilClimbingCapability;
    private TextInputLayout tilBelayerNotes;

    private TextInputEditText etBelayerName;
    private TextInputEditText etBelayerWall;
    private TextInputEditText etBelayerDays;
    private TextInputEditText etBelayerTimes;
    private AutoCompleteTextView actvBelayerCapability;
    private TextInputEditText etClimbingCapability;
    private TextInputEditText etBelayerNotes;
    private MaterialButton btnPublishBelayerPost;

    private TextView tvBelayerPostsValue;
    private TextView tvBelayerWallsValue;
    private TextView tvBelayerPostsEmpty;
    private RecyclerView rvBelayerPosts;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private BelayerPostAdapter belayerPostAdapter;
    private ListenerRegistration belayerPostsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_belayer);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        bindViews();
        configureToolbar();
        configureBelayerCapabilityDropdown();
        configureBelayerPostsList();
        prefillDisplayName();

        btnPublishBelayerPost.setOnClickListener(view -> publishBelayerPost());
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        listenForBelayerPosts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (belayerPostsListener != null) {
            belayerPostsListener.remove();
            belayerPostsListener = null;
        }
    }

    private void bindViews() {
        tilBelayerName = findViewById(R.id.tilBelayerName);
        tilBelayerWall = findViewById(R.id.tilBelayerWall);
        tilBelayerDays = findViewById(R.id.tilBelayerDays);
        tilBelayerTimes = findViewById(R.id.tilBelayerTimes);
        tilBelayerCapability = findViewById(R.id.tilBelayerCapability);
        tilClimbingCapability = findViewById(R.id.tilClimbingCapability);
        tilBelayerNotes = findViewById(R.id.tilBelayerNotes);

        etBelayerName = findViewById(R.id.etBelayerName);
        etBelayerWall = findViewById(R.id.etBelayerWall);
        etBelayerDays = findViewById(R.id.etBelayerDays);
        etBelayerTimes = findViewById(R.id.etBelayerTimes);
        actvBelayerCapability = findViewById(R.id.actvBelayerCapability);
        etClimbingCapability = findViewById(R.id.etClimbingCapability);
        etBelayerNotes = findViewById(R.id.etBelayerNotes);
        btnPublishBelayerPost = findViewById(R.id.btnPublishBelayerPost);

        tvBelayerPostsValue = findViewById(R.id.tvBelayerPostsValue);
        tvBelayerWallsValue = findViewById(R.id.tvBelayerWallsValue);
        tvBelayerPostsEmpty = findViewById(R.id.tvBelayerPostsEmpty);
        rvBelayerPosts = findViewById(R.id.rvBelayerPosts);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarFindBelayer);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void configureBelayerCapabilityDropdown() {
        String[] capabilities = getResources().getStringArray(R.array.belay_capability_options);
        ArrayAdapter<String> capabilityAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, capabilities);
        actvBelayerCapability.setAdapter(capabilityAdapter);
        if (capabilities.length > 0) {
            actvBelayerCapability.setText(capabilities[0], false);
        }
    }

    private void configureBelayerPostsList() {
        rvBelayerPosts.setLayoutManager(new LinearLayoutManager(this));
        belayerPostAdapter = new BelayerPostAdapter(
                getLayoutInflater(),
                new BelayerPostAdapter.ActionListener() {
                    @Override
                    public void onMessage(BelayerPost post) {
                        openConversation(post);
                    }

                    @Override
                    public void onDeletePost(BelayerPost post) {
                        confirmDeletePost(post);
                    }

                    @Override
                    public void onViewProfile(BelayerPost post) {
                        openProfile(post);
                    }
                },
                currentUser != null ? currentUser.getUid() : ""
        );
        rvBelayerPosts.setAdapter(belayerPostAdapter);
    }

    private void prefillDisplayName() {
        if (!TextUtils.isEmpty(valueOf(etBelayerName))) {
            return;
        }

        String displayName = currentUser.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = currentUser.getEmail();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.find_belayer_default_name);
        }

        etBelayerName.setText(displayName);
    }

    private void publishBelayerPost() {
        if (isServerAccessBlocked()) {
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        clearInputErrors();

        String displayName = valueOf(etBelayerName);
        String wallName = valueOf(etBelayerWall);
        String climbDays = valueOf(etBelayerDays);
        String climbTimes = valueOf(etBelayerTimes);
        String belayCapability = valueOf(actvBelayerCapability);
        String climbCapability = valueOf(etClimbingCapability);
        String notes = valueOf(etBelayerNotes);

        boolean hasError = false;
        if (TextUtils.isEmpty(wallName)) {
            tilBelayerWall.setError(getString(R.string.find_belayer_wall_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbDays)) {
            tilBelayerDays.setError(getString(R.string.find_belayer_days_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbTimes)) {
            tilBelayerTimes.setError(getString(R.string.find_belayer_times_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(belayCapability)) {
            tilBelayerCapability.setError(getString(R.string.find_belayer_belay_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbCapability)) {
            tilClimbingCapability.setError(getString(R.string.find_belayer_climb_required));
            hasError = true;
        }
        if (hasError) {
            return;
        }

        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.find_belayer_default_name);
        }

        btnPublishBelayerPost.setEnabled(false);
        Map<String, Object> postData = new HashMap<>();
        postData.put("authorUid", currentUser.getUid());
        postData.put("authorName", displayName);
        postData.put("wallName", wallName);
        postData.put("climbDays", climbDays);
        postData.put("climbTimes", climbTimes);
        postData.put("belayCapability", belayCapability);
        postData.put("climbCapability", climbCapability);
        postData.put("notes", notes);
        postData.put("createdAt", FieldValue.serverTimestamp());

        if (isServerAccessBlocked()) {
            btnPublishBelayerPost.setEnabled(true);
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .add(postData)
                .addOnSuccessListener(documentReference -> {
                    btnPublishBelayerPost.setEnabled(true);
                    clearPostForm();
                    Toast.makeText(this, R.string.find_belayer_saved_toast, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnPublishBelayerPost.setEnabled(true);
                    Toast.makeText(this, getString(R.string.find_belayer_save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForBelayerPosts() {
        if (belayerPostsListener != null) {
            belayerPostsListener.remove();
        }

        tvBelayerPostsEmpty.setVisibility(TextView.VISIBLE);
        tvBelayerPostsEmpty.setText(R.string.find_belayer_loading_history);
        rvBelayerPosts.setVisibility(RecyclerView.GONE);

        belayerPostsListener = firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        rvBelayerPosts.setVisibility(RecyclerView.GONE);
                        tvBelayerPostsEmpty.setVisibility(TextView.VISIBLE);
                        tvBelayerPostsEmpty.setText(getString(R.string.find_belayer_load_failed, error.getMessage()));
                        return;
                    }

                    List<BelayerPost> posts = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            BelayerPost post = documentSnapshot.toObject(BelayerPost.class);
                            if (post != null) {
                                post.setId(documentSnapshot.getId());
                                posts.add(post);
                            }
                        });
                    }
                    posts.sort(Comparator.comparing(
                            BelayerPost::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());

                    belayerPostAdapter.submitList(posts);
                    updateSummary(posts);

                    boolean hasPosts = !posts.isEmpty();
                    rvBelayerPosts.setVisibility(hasPosts ? RecyclerView.VISIBLE : RecyclerView.GONE);
                    tvBelayerPostsEmpty.setVisibility(hasPosts ? TextView.GONE : TextView.VISIBLE);
                    if (!hasPosts) {
                        tvBelayerPostsEmpty.setText(R.string.find_belayer_empty_history);
                    }
                });
    }

    private void updateSummary(List<BelayerPost> posts) {
        Set<String> uniqueWalls = new HashSet<>();
        for (BelayerPost post : posts) {
            uniqueWalls.add(post.getWallName().trim().toLowerCase(Locale.getDefault()));
        }

        tvBelayerPostsValue.setText(getString(R.string.find_belayer_total_posts_value, posts.size()));
        tvBelayerWallsValue.setText(getString(R.string.find_belayer_total_walls_value, uniqueWalls.size()));
    }

    private void openConversation(BelayerPost post) {
        if (TextUtils.isEmpty(post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_message_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser != null && TextUtils.equals(currentUser.getUid(), post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_message_self_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, post.getAuthorUid());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, post.getAuthorName());
        startActivity(intent);
    }

    private void openProfile(BelayerPost post) {
        if (TextUtils.isEmpty(post.getAuthorUid())) {
            return;
        }

        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", post.getAuthorUid());
        startActivity(intent);
    }

    private void confirmDeletePost(BelayerPost post) {
        if (TextUtils.isEmpty(post.getId())) {
            Toast.makeText(this, R.string.find_belayer_delete_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null || !TextUtils.equals(currentUser.getUid(), post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_delete_not_owner, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.find_belayer_delete_title)
                .setMessage(R.string.find_belayer_delete_message)
                .setPositiveButton(R.string.find_belayer_delete_confirm, (dialog, which) -> deletePost(post.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deletePost(String postId) {
        firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .document(postId)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.find_belayer_delete_success, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.find_belayer_delete_failed, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }

    private void clearInputErrors() {
        tilBelayerName.setError(null);
        tilBelayerWall.setError(null);
        tilBelayerDays.setError(null);
        tilBelayerTimes.setError(null);
        tilBelayerCapability.setError(null);
        tilClimbingCapability.setError(null);
        tilBelayerNotes.setError(null);
    }

    private void clearPostForm() {
        etBelayerWall.setText("");
        etBelayerDays.setText("");
        etBelayerTimes.setText("");
        etClimbingCapability.setText("");
        etBelayerNotes.setText("");

        String[] capabilities = getResources().getStringArray(R.array.belay_capability_options);
        if (capabilities.length > 0) {
            actvBelayerCapability.setText(capabilities[0], false);
        } else {
            actvBelayerCapability.setText("");
        }
    }

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }

    private boolean isServerAccessBlocked() {
        return ServerFeatureGate.isServerFeatureBlocked(this);
    }
}
