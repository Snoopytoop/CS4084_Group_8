package com.example.cs4084_group_8;

import android.content.Intent;
import android.text.InputType;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class HomeActivity extends AppCompatActivity {
    private static final String PROFILE_CACHE_PREF = "profile_cache";
    private static final String PROFILE_IMAGE_URL_PREFIX = "profile_image_url_";

    private ShapeableImageView ivNavProfile;
    private ImageButton btnNavCreatePost;
    private RecyclerView rvPosts;
    private TextView tvEmptyFeed;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private PostAdapter postAdapter;
    private ListenerRegistration postsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ImageButton btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ivNavProfile = findViewById(R.id.ivNavProfile);
        btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        rvPosts = findViewById(R.id.rvPosts);
        tvEmptyFeed = findViewById(R.id.tvEmptyFeed);

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
        btnNavLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
        ivNavProfile.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));
        btnNavCreatePost.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUser = user;
        if (user != null) {
            loadNavProfileImage(user.getUid(), ivNavProfile);
            listenForPosts();
        } else {
            ivNavProfile.setImageResource(android.R.drawable.ic_menu_camera);
            tvEmptyFeed.setText("Please log in to view and create posts.");
            tvEmptyFeed.setVisibility(View.VISIBLE);
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
        firestore.collection("posts")
                .document(post.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    Post latestPost = snapshot.toObject(Post.class);
                    if (latestPost == null) {
                        return;
                    }
                    List<String> likedBy = latestPost.getLikedBy();
                    boolean alreadyLiked = likedBy.contains(currentUser.getUid());

                    Map<String, Object> updates = new HashMap<>();
                    if (alreadyLiked) {
                        updates.put("likedBy", FieldValue.arrayRemove(currentUser.getUid()));
                        updates.put("likesCount", FieldValue.increment(-1));
                    } else {
                        updates.put("likedBy", FieldValue.arrayUnion(currentUser.getUid()));
                        updates.put("likesCount", FieldValue.increment(1));
                    }

                    firestore.collection("posts")
                            .document(post.getId())
                            .update(updates);
                });
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
            if (currentUser == null) {
                dialog.dismiss();
                return;
            }
            String commentText = etComment.getText() == null ? "" : etComment.getText().toString().trim();
            if (TextUtils.isEmpty(commentText)) {
                etComment.setError("Comment cannot be empty");
                return;
            }

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

                        firestore.collection("posts")
                                .document(post.getId())
                                .collection("comments")
                                .add(commentData)
                                .addOnSuccessListener(unused -> firestore.collection("posts")
                                        .document(post.getId())
                                        .update("commentsCount", FieldValue.increment(1))
                                        .addOnSuccessListener(unused2 -> dialog.dismiss())
                                        .addOnFailureListener(e -> Toast.makeText(this, "Comment saved, count update failed.", Toast.LENGTH_SHORT).show()))
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
        }));
        dialog.show();
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
}
