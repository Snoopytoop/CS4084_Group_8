package com.example.cs4084_group_8;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreatePostActivity extends AppCompatActivity {
    private static final int BLOG_CHAR_THRESHOLD = 260;
    private static final long MAX_POST_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)", Pattern.CASE_INSENSITIVE);

    private TextInputLayout tilPostContent;
    private TextInputEditText etPostContent;
    private ImageView ivSelectedPostImage;
    private MaterialButton btnPickPostImage;
    private MaterialButton btnPublishPost;
    private ShapeableImageView ivNavProfile;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String validationError = ImageValidation.validateImageSelection(this, uri, MAX_POST_IMAGE_BYTES);
                    if (validationError != null) {
                        selectedImageUri = null;
                        ivSelectedPostImage.setVisibility(ImageView.GONE);
                        btnPickPostImage.setText("Add Photo");
                        Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                        return;
                    }
                    selectedImageUri = uri;
                    ivSelectedPostImage.setImageURI(uri);
                    ivSelectedPostImage.setVisibility(ImageView.VISIBLE);
                    btnPickPostImage.setText("Change Photo");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Adjust nav bar position for gesture/button navigation
        View bottomNav = findViewById(R.id.bottomNavCard);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = 16 + bottomInset;
            v.setLayoutParams(params);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to create a post.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupNavigation();
        btnPickPostImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnPublishPost.setOnClickListener(v -> publishPost());
    }

    private void bindViews() {
        tilPostContent = findViewById(R.id.tilPostContent);
        etPostContent = findViewById(R.id.etPostContent);
        ivSelectedPostImage = findViewById(R.id.ivSelectedPostImage);
        btnPickPostImage = findViewById(R.id.btnPickPostImage);
        btnPublishPost = findViewById(R.id.btnPublishPost);
    }

    private void setupNavigation() {
        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ImageButton btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ImageButton btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
        });
        btnNavLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            overridePendingTransition(0, 0);
        });
        btnNavCreatePost.setOnClickListener(v -> {
            // Already on create post
        });
        ivNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void publishPost() {
        clearErrors();

        String content = valueOf(etPostContent);
        if (TextUtils.isEmpty(content)) {
            tilPostContent.setError("Post content is required");
            return;
        }

        setLoading(true);
        if (selectedImageUri != null) {
            uploadSelectedImageAndCreatePost(content);
        } else {
            String inferredType = inferPostType(content);
            String extractedUrl = extractFirstUrl(content);
            createPostDocument(content, inferredType, extractedUrl);
        }
    }

    private void uploadSelectedImageAndCreatePost(String content) {
        String validationError = ImageValidation.validateImageSelection(this, selectedImageUri, MAX_POST_IMAGE_BYTES);
        if (validationError != null) {
            setLoading(false);
            Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
            return;
        }

        String filename = currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child("post_images")
                .child(filename);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri ->
                                createPostDocument(content, "image", downloadUri.toString()))
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, "Image uploaded but URL fetch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createPostDocument(String content, String postType, String mediaUrl) {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String authorName = snapshot.getString("username");
                    String authorProfileImageUrl = snapshot.getString("profileImageUrl");

                    Map<String, Object> postData = new HashMap<>();
                    postData.put("authorUid", currentUser.getUid());
                    postData.put("authorName", TextUtils.isEmpty(authorName) ? "Unknown user" : authorName);
                    postData.put("authorProfileImageUrl", authorProfileImageUrl);
                    postData.put("postType", postType.toLowerCase());
                    postData.put("content", content);
                    postData.put("mediaUrl", mediaUrl);
                    postData.put("likesCount", 0);
                    postData.put("commentsCount", 0);
                    postData.put("likedBy", new ArrayList<String>());
                    postData.put("createdAt", FieldValue.serverTimestamp());

                    firestore.collection("posts")
                            .add(postData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Post created", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Failed to create post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        tilPostContent.setError(null);
    }

    private void setLoading(boolean isLoading) {
        btnPickPostImage.setEnabled(!isLoading);
        btnPublishPost.setEnabled(!isLoading);
    }

    private String valueOf(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private String inferPostType(String content) {
        String firstUrl = extractFirstUrl(content);
        if (!TextUtils.isEmpty(firstUrl)) {
            String normalized = firstUrl.toLowerCase();
            if (normalized.contains("youtube.com")
                    || normalized.contains("youtu.be")
                    || normalized.contains("vimeo.com")
                    || normalized.endsWith(".mp4")
                    || normalized.endsWith(".mov")
                    || normalized.endsWith(".webm")) {
                return "video";
            }
        }
        return content.length() >= BLOG_CHAR_THRESHOLD ? "blog" : "status";
    }

    private String extractFirstUrl(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
