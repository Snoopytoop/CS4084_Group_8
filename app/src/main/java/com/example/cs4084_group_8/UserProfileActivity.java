package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {
    private static final int MENU_EDIT_PROFILE_ID = 1001;
    private static final int MENU_LOGOUT_ID = 1002;
    private static final String PROFILE_CACHE_PREF = "profile_cache";
    private static final String PROFILE_IMAGE_URL_PREFIX = "profile_image_url_";

    private ImageView ivProfile;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvBio;
    private ImageButton btnSettings;
    private ImageButton btnCreatePostTop;
    private ImageButton btnNavHome;
    private ImageButton btnNavCreatePost;
    private ImageButton btnNavLeaderboard;
    private ShapeableImageView ivNavProfile;
    private RecyclerView rvMyPosts;
    private TextView tvEmptyMyPosts;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private PostAdapter postAdapter;
    private ListenerRegistration myPostsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ivProfile = findViewById(R.id.ivProfile);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);
        btnSettings = findViewById(R.id.btnSettings);
        btnCreatePostTop = findViewById(R.id.btnCreatePostTop);
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ivNavProfile = findViewById(R.id.ivNavProfile);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        tvEmptyMyPosts = findViewById(R.id.tvEmptyMyPosts);

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
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        rvMyPosts.setAdapter(postAdapter);

        btnSettings.setOnClickListener(v -> showSettingsMenu());
        btnCreatePostTop.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            overridePendingTransition(0, 0);
        });
        btnNavCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
            overridePendingTransition(0, 0);
        });
        btnNavLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            overridePendingTransition(0, 0);
        });
        ivNavProfile.setOnClickListener(v -> {
            // Already on profile.
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myPostsListener != null) {
            myPostsListener.remove();
            myPostsListener = null;
        }
    }

    private void loadUserProfile() {
        String userIdFromIntent = getIntent().getStringExtra("USER_ID");
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();

        // Decide whose profile to load
        String targetUserId;
        if (!TextUtils.isEmpty(userIdFromIntent)) {
            targetUserId = userIdFromIntent; // viewing another user
        } else if (loggedInUser != null) {
            targetUserId = loggedInUser.getUid(); // viewing own profile
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // If viewing own profile → show email, else hide it
        if (loggedInUser != null && targetUserId.equals(loggedInUser.getUid())) {
            // Viewing your own profile
            tvEmail.setText(loggedInUser.getEmail() == null ? "" : loggedInUser.getEmail());
            tvEmail.setVisibility(View.VISIBLE);
            btnSettings.setVisibility(View.VISIBLE); // show settings
        } else {
            // Viewing someone else's profile
            tvEmail.setVisibility(View.GONE);
            btnSettings.setVisibility(View.GONE); // hide settings
        }

        // Load user data from Firestore
        firestore.collection("users")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    String bio = snapshot.getString("bio");
                    String imageUrl = snapshot.getString("profileImageUrl");

                    tvUsername.setText(TextUtils.isEmpty(username) ? "No username yet" : username);
                    tvBio.setText(TextUtils.isEmpty(bio) ? "No bio yet" : bio);

                    if (!TextUtils.isEmpty(imageUrl)) {
                        loadProfileImages(imageUrl);
                    } else {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_camera);
                        ivNavProfile.setImageResource(android.R.drawable.ic_menu_camera);
                    }

                    // Load THAT user's posts
                    listenForMyPosts(targetUserId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void listenForMyPosts(String uid) {
        if (myPostsListener != null) {
            myPostsListener.remove();
        }
        myPostsListener = firestore.collection("posts")
                .whereEqualTo("authorUid", uid)
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
                    Collections.sort(posts, Comparator.comparing(Post::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                    postAdapter.submitList(posts);
                    tvEmptyMyPosts.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
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
        View dialogView = inflater.inflate(R.layout.dialog_add_comment, null);
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

    private void loadProfileImages(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                    .edit()
                    .putString(PROFILE_IMAGE_URL_PREFIX + user.getUid(), imageUrl)
                    .apply();
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(ivProfile);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(ivNavProfile);
    }

    private void showSettingsMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnSettings);
        popupMenu.getMenu().add(0, MENU_EDIT_PROFILE_ID, 0, "Edit profile");
        popupMenu.getMenu().add(0, MENU_LOGOUT_ID, 1, "Logout");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_EDIT_PROFILE_ID) {
                Intent intent = new Intent(this, ProfileSetupActivity.class);
                intent.putExtra("fromProfile", true);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == MENU_LOGOUT_ID) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
