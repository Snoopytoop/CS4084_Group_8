package com.example.cs4084_group_8;

import android.content.Intent;
import android.text.InputType;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static final String PROFILE_CACHE_PREF = "profile_cache";
    private static final String PROFILE_IMAGE_URL_PREFIX = "profile_image_url_";

    private ShapeableImageView ivNavProfile;
    private ImageButton btnNavCreatePost;
    private ImageButton btnNavSearch;
    private ImageButton btnNavLeaderboard;
    private MaterialButton btnQuickRouteLog;
    private MaterialButton btnQuickMessages;
    private MaterialButton btnQuickFindBelayer;
    private MaterialButton btnQuickBlogs;
    private MaterialButton btnRetryConnection;
    private RecyclerView rvPosts;
    private TextView tvEmptyFeed;
    private TextView tvOfflineChip;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private boolean firebaseServerReachable;
    private boolean firebaseProbeInProgress;
    private PostAdapter postAdapter;
    private ListenerRegistration postsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        btnNavSearch = findViewById(R.id.btnNavSearch);
        btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ivNavProfile = findViewById(R.id.ivNavProfile);
        btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        btnQuickRouteLog = findViewById(R.id.btnQuickRouteLog);
        btnQuickMessages = findViewById(R.id.btnQuickMessages);
        btnQuickFindBelayer = findViewById(R.id.btnQuickFindBelayer);
        btnQuickBlogs = findViewById(R.id.btnQuickBlogs);
        btnRetryConnection = findViewById(R.id.btnRetryConnection);
        rvPosts = findViewById(R.id.rvPosts);
        tvEmptyFeed = findViewById(R.id.tvEmptyFeed);
        tvOfflineChip = findViewById(R.id.tvOfflineChip);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        postAdapter = new PostAdapter(
                currentUser != null ? currentUser.getUid() : "",
                new PostAdapter.PostActionListener() {
                    @Override
                    public void onLikeClick(Post post) {
                        toggleLike(post);
                    }

                    @Override
                    public void onCommentClick(Post post) {
                        showCommentDialog(post);
                    }
                }
        );
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);

        btnNavHome.setOnClickListener(v -> {
            // Already on home.
        });
        btnNavSearch.setOnClickListener(v -> openServerFeature(SearchActivity.class));
        btnNavLeaderboard.setOnClickListener(v -> openServerFeature(LeaderboardActivity.class));
        ivNavProfile.setOnClickListener(v -> openServerFeature(UserProfileActivity.class));
        btnNavCreatePost.setOnClickListener(v -> openServerFeature(CreatePostActivity.class));
        btnQuickRouteLog.setOnClickListener(v -> startActivity(new Intent(this, RouteLogActivity.class)));
        btnQuickMessages.setOnClickListener(v -> openServerFeature(InboxActivity.class));
        btnQuickFindBelayer.setOnClickListener(v -> openServerFeature(FindBelayerActivity.class));
        btnQuickBlogs.setOnClickListener(v -> openServerFeature(BlogsActivity.class));
        btnRetryConnection.setOnClickListener(v -> retryOnlineConnection());

        // Adjust nav bar position for gesture/button navigation
        View bottomNav = findViewById(R.id.bottomNavCard);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = 16 + bottomInset;
            v.setLayoutParams(params);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUser = user;
        hideOfflineIndicators();

        if (isForcedOfflineSession()) {
            applyOfflineState();
            return;
        }

        if (user == null) {
            applyLoggedOutState();
            return;
        }

        // Check local network first; if available, assume online immediately
        if (NetworkStatus.isOnline(this)) {
            firebaseServerReachable = true;
            applyOnlineState(user);
            // Verify Firebase in background; only revert if probe fails
            verifyFirebaseServerInitial(user);
        } else {
            // No local network, definitely offline
            firebaseServerReachable = false;
            applyOfflineState();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }

    private void listenForPosts() {
        if (postsListener != null) {
            postsListener.remove();
        }
        postsListener = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load posts: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<Post> posts = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            Post post = documentSnapshot.toObject(Post.class);
                            if (post != null) {
                                post.setId(documentSnapshot.getId());
                                posts.add(post);
                            }
                        });
                    }
                    postAdapter.submitList(posts);
                    tvEmptyFeed.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void toggleLike(Post post) {
        if (currentUser == null || TextUtils.isEmpty(post.getId())) {
            return;
        }
        DocumentReference postRef = firestore.collection("posts").document(post.getId());
        firestore.runTransaction(transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(postRef);
                    Post latestPost = snapshot.toObject(Post.class);
                    if (latestPost == null) {
                        throw new IllegalStateException("Post not found");
                    }
                    List<String> likedBy = latestPost.getLikedBy();
                    boolean alreadyLiked = likedBy.contains(currentUser.getUid());
                    long currentLikes = Math.max(0, latestPost.getLikesCount());
                    long updatedLikes = alreadyLiked ? Math.max(0, currentLikes - 1) : currentLikes + 1;

                    transaction.update(postRef, "likedBy",
                            alreadyLiked
                                    ? FieldValue.arrayRemove(currentUser.getUid())
                                    : FieldValue.arrayUnion(currentUser.getUid()));
                    transaction.update(postRef, "likesCount", updatedLikes);
                    return null;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update like: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showCommentDialog(Post post) {
        if (TextUtils.isEmpty(post.getId())) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View dialogView = inflater.inflate(R.layout.dialog_add_comment, null);
        TextView tvExistingComments = dialogView.findViewById(R.id.tvExistingComments);
        EditText etComment = dialogView.findViewById(R.id.etComment);
        etComment.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        firestore.collection("posts")
                .document(post.getId())
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvExistingComments.setText("No comments yet.");
                    } else {
                        StringBuilder builder = new StringBuilder();
                        queryDocumentSnapshots.getDocuments().forEach(commentDoc -> {
                            String authorName = commentDoc.getString("authorName");
                            String text = commentDoc.getString("text");
                            if (!TextUtils.isEmpty(text)) {
                                builder.append(TextUtils.isEmpty(authorName) ? "User" : authorName)
                                        .append(": ")
                                        .append(text)
                                        .append("\n\n");
                            }
                        });
                        tvExistingComments.setText(builder.toString().trim());
                    }
                });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Comments")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Post", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            android.widget.Button postButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (currentUser == null) {
                dialog.dismiss();
                return;
            }
            String commentText = etComment.getText() == null ? "" : etComment.getText().toString().trim();
            if (TextUtils.isEmpty(commentText)) {
                etComment.setError("Comment cannot be empty");
                return;
            }

            postButton.setEnabled(false);
            firestore.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        String authorName = userSnapshot.getString("username");
                        Map<String, Object> commentData = new HashMap<>();
                        commentData.put("authorUid", currentUser.getUid());
                        commentData.put("authorName", TextUtils.isEmpty(authorName) ? "User" : authorName);
                        commentData.put("text", commentText);
                        commentData.put("createdAt", FieldValue.serverTimestamp());

                        DocumentReference commentRef = firestore.collection("posts")
                                .document(post.getId())
                                .collection("comments")
                                .document();
                        DocumentReference postRef = firestore.collection("posts").document(post.getId());
                        firestore.runBatch(batch -> {
                                    batch.set(commentRef, commentData);
                                    batch.update(postRef, "commentsCount", FieldValue.increment(1));
                                })
                                .addOnSuccessListener(unused -> dialog.dismiss())
                                .addOnFailureListener(e -> {
                                    postButton.setEnabled(true);
                                    Toast.makeText(this, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        postButton.setEnabled(true);
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }));
        dialog.show();
    }

    private void openServerFeature(Class<?> activityClass) {
        // Check connectivity fresh every time - don't rely on stale flag
        if (!NetworkStatus.isOnline(this)) {
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(this, activityClass));
        overridePendingTransition(0, 0);
    }

    private void enableOfflineUi() {
        setDisabledUiState(btnNavSearch);
        setDisabledUiState(btnNavLeaderboard);
        setDisabledUiState(btnNavCreatePost);
        setDisabledUiState(btnQuickMessages);
        setDisabledUiState(btnQuickFindBelayer);
        setDisabledUiState(btnQuickBlogs);
        setDisabledUiState(ivNavProfile);
    }

    private void enableOnlineUi() {
        setEnabledUiState(btnNavSearch);
        setEnabledUiState(btnNavLeaderboard);
        setEnabledUiState(btnNavCreatePost);
        setEnabledUiState(btnQuickMessages);
        setEnabledUiState(btnQuickFindBelayer);
        setEnabledUiState(btnQuickBlogs);
        setEnabledUiState(ivNavProfile);
    }

    private void setDisabledUiState(View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(false);
        view.setAlpha(0.4f);
    }

    private void setEnabledUiState(View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(true);
        view.setAlpha(1f);
    }

    private void loadNavProfileImage(String uid, ImageView targetView) {
        String cachedUrl = getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                .getString(PROFILE_IMAGE_URL_PREFIX + uid, null);
        if (!TextUtils.isEmpty(cachedUrl)) {
            renderProfileImage(cachedUrl, targetView);
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String imageUrl = snapshot.getString("profileImageUrl");
                    if (TextUtils.isEmpty(imageUrl)) {
                        targetView.setImageResource(android.R.drawable.ic_menu_camera);
                        return;
                    }
                    getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                            .edit()
                            .putString(PROFILE_IMAGE_URL_PREFIX + uid, imageUrl)
                            .apply();
                    renderProfileImage(imageUrl, targetView);
                })
                .addOnFailureListener(e -> targetView.setImageResource(android.R.drawable.ic_menu_camera));
    }

    private void renderProfileImage(String imageUrl, ImageView targetView) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(targetView);
    }

    private void retryOnlineConnection() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUser = user;

        if (isForcedOfflineSession()) {
            Toast.makeText(this, R.string.home_retry_still_offline, Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null) {
            Toast.makeText(this, R.string.home_retry_already_online, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkStatus.isOnline(this)) {
            Toast.makeText(this, R.string.home_retry_still_offline, Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic: set online immediately when local network available, then verify Firebase
        firebaseServerReachable = true;
        applyOnlineState(user);
        Toast.makeText(this, R.string.home_retry_online_success, Toast.LENGTH_SHORT).show();
        
        // Verify Firebase in background; only revert if probe fails
        verifyFirebaseServerAfterRetry(user);
    }

    private void verifyFirebaseServerAfterRetry(FirebaseUser user) {
        if (firebaseProbeInProgress) {
            return;
        }
        firebaseProbeInProgress = true;

        firestore.collection(FirestoreCollections.USERS)
                .document(user.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    firebaseProbeInProgress = false;
                    firebaseServerReachable = true;
                })
                .addOnFailureListener(e -> {
                    firebaseProbeInProgress = false;
                    firebaseServerReachable = false;
                    applyOfflineState();
                    Toast.makeText(this, R.string.home_retry_still_offline, Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyFirebaseServerInitial(FirebaseUser user) {
        if (firebaseProbeInProgress) {
            return;
        }
        firebaseProbeInProgress = true;

        firestore.collection(FirestoreCollections.USERS)
                .document(user.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    firebaseProbeInProgress = false;
                    firebaseServerReachable = true;
                })
                .addOnFailureListener(e -> {
                    firebaseProbeInProgress = false;
                    firebaseServerReachable = false;
                    applyOfflineState();
                });
    }

    private void applyOnlineState(FirebaseUser user) {
        hideOfflineIndicators();
        loadNavProfileImage(user.getUid(), ivNavProfile);
        enableOnlineUi();
        rvPosts.setVisibility(View.VISIBLE);
        
        // Load fresh server data first to avoid showing stale cache
        postAdapter.submitList(new ArrayList<>());
        tvEmptyFeed.setVisibility(View.VISIBLE);
        tvEmptyFeed.setText(R.string.home_loading_posts);
        
        firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    List<Post> posts = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(documentSnapshot -> {
                        Post post = documentSnapshot.toObject(Post.class);
                        if (post != null) {
                            post.setId(documentSnapshot.getId());
                            posts.add(post);
                        }
                    });
                    postAdapter.submitList(posts);
                    tvEmptyFeed.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                    // Now set up real-time listener for updates
                    listenForPosts();
                })
                .addOnFailureListener(e -> {
                    // Fall back to listener even if server load fails
                    listenForPosts();
                });
    }

    private void applyOfflineState() {
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        firebaseServerReachable = false;
        postAdapter.submitList(new ArrayList<>());
        ivNavProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
        tvEmptyFeed.setVisibility(View.VISIBLE);
        tvEmptyFeed.setText(R.string.home_offline_mode_empty_state);
        enableOfflineUi();
        showOfflineIndicators();
        rvPosts.setVisibility(View.GONE);
    }

    private void applyLoggedOutState() {
        firebaseServerReachable = false;
        ivNavProfile.setImageResource(android.R.drawable.ic_menu_camera);
        tvEmptyFeed.setText("Please log in to view and create posts.");
        tvEmptyFeed.setVisibility(View.VISIBLE);
        enableOfflineUi();
        hideOfflineIndicators();
        rvPosts.setVisibility(View.GONE);
    }

    private void hideOfflineIndicators() {
        tvOfflineChip.setVisibility(View.GONE);
        btnRetryConnection.setVisibility(View.GONE);
    }

    private void showOfflineIndicators() {
        tvOfflineChip.setVisibility(View.VISIBLE);
        tvOfflineChip.setText(R.string.home_offline_chip);
        btnRetryConnection.setVisibility(View.VISIBLE);
    }

    private boolean isForcedOfflineSession() {
        return OfflineSessionManager.isOfflineModeEnabled(this)
                && FirebaseAuth.getInstance().getCurrentUser() == null;
    }
}
