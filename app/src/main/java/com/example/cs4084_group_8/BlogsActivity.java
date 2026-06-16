package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlogsActivity extends AppCompatActivity {
    private TextInputLayout tilBlogTitle;
    private TextInputLayout tilBlogBody;
    private TextInputEditText etBlogTitle;
    private TextInputEditText etBlogBody;
    private MaterialButton btnPublishBlog;
    private TextView tvBlogsEmptyState;
    private RecyclerView rvBlogs;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private BlogPostAdapter blogPostAdapter;
    private ListenerRegistration blogsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blogs);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage your blogs.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        bindViews();
        configureToolbar();
        configureBlogList();

        btnPublishBlog.setOnClickListener(v -> publishBlog());
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

        listenForBlogs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (blogsListener != null) {
            blogsListener.remove();
            blogsListener = null;
        }
    }

    private void bindViews() {
        tilBlogTitle = findViewById(R.id.tilBlogTitle);
        tilBlogBody = findViewById(R.id.tilBlogBody);
        etBlogTitle = findViewById(R.id.etBlogTitle);
        etBlogBody = findViewById(R.id.etBlogBody);
        btnPublishBlog = findViewById(R.id.btnPublishBlog);
        tvBlogsEmptyState = findViewById(R.id.tvBlogsEmptyState);
        rvBlogs = findViewById(R.id.rvBlogs);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarBlogs);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void configureBlogList() {
        rvBlogs.setLayoutManager(new LinearLayoutManager(this));
        blogPostAdapter = new BlogPostAdapter(getLayoutInflater(), new BlogPostAdapter.ActionListener() {
            @Override
            public void onDeletePost(BlogPost post) {
                confirmDeletePost(post);
            }

            @Override
            public void onViewProfile(BlogPost post) {
                openAuthorProfile(post);
            }
        }, currentUser.getUid());
        rvBlogs.setAdapter(blogPostAdapter);
    }

    private void publishBlog() {
        if (isServerAccessBlocked()) {
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        clearErrors();

        String title = BlogValidation.normalizeTitle(valueOf(etBlogTitle));
        String body = BlogValidation.normalizeBody(valueOf(etBlogBody));

        boolean hasError = false;
        if (!BlogValidation.isTitleValid(title)) {
            tilBlogTitle.setError(TextUtils.isEmpty(title)
                    ? getString(R.string.blogs_title_required)
                    : getString(R.string.blogs_title_too_long));
            hasError = true;
        }
        if (!BlogValidation.isBodyValid(body)) {
            tilBlogBody.setError(TextUtils.isEmpty(body)
                    ? getString(R.string.blogs_body_required)
                    : getString(R.string.blogs_body_too_long));
            hasError = true;
        }
        if (hasError) {
            return;
        }

        setPublishing(true);
        firestore.collection(FirestoreCollections.USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String authorName = snapshot.getString("username");
                    if (TextUtils.isEmpty(authorName)) {
                        authorName = currentUser.getDisplayName();
                    }
                    if (TextUtils.isEmpty(authorName)) {
                        authorName = getString(R.string.blogs_default_author_name);
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("authorUid", currentUser.getUid());
                    data.put("authorName", authorName);
                    data.put("title", title);
                    data.put("body", body);
                    data.put("createdAt", FieldValue.serverTimestamp());

                    if (isServerAccessBlocked()) {
                        setPublishing(false);
                        Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection(FirestoreCollections.BLOGS)
                            .add(data)
                            .addOnSuccessListener(documentReference -> {
                                setPublishing(false);
                                clearInputFields();
                                Toast.makeText(this, R.string.blogs_saved_toast, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                setPublishing(false);
                                Toast.makeText(this, getString(R.string.blogs_save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setPublishing(false);
                    Toast.makeText(this, getString(R.string.blogs_profile_load_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForBlogs() {
        if (blogsListener != null) {
            blogsListener.remove();
        }

        tvBlogsEmptyState.setVisibility(View.VISIBLE);
        tvBlogsEmptyState.setText(R.string.blogs_loading_history);
        rvBlogs.setVisibility(View.GONE);

        blogsListener = firestore.collection(FirestoreCollections.BLOGS)
                .whereEqualTo("authorUid", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        rvBlogs.setVisibility(View.GONE);
                        tvBlogsEmptyState.setVisibility(View.VISIBLE);
                        tvBlogsEmptyState.setText(getString(R.string.blogs_load_failed, error.getMessage()));
                        return;
                    }

                    List<BlogPost> blogs = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            BlogPost blog = documentSnapshot.toObject(BlogPost.class);
                            if (blog != null) {
                                blog.setId(documentSnapshot.getId());
                                blogs.add(blog);
                            }
                        });
                    }

                    blogs.sort(new Comparator<BlogPost>() {
                        @Override
                        public int compare(BlogPost left, BlogPost right) {
                            Timestamp leftTime = left.getCreatedAt();
                            Timestamp rightTime = right.getCreatedAt();
                            if (leftTime == null && rightTime == null) {
                                return 0;
                            }
                            if (leftTime == null) {
                                return 1;
                            }
                            if (rightTime == null) {
                                return -1;
                            }
                            return rightTime.compareTo(leftTime);
                        }
                    });

                    blogPostAdapter.submitList(blogs);
                    rvBlogs.setVisibility(blogs.isEmpty() ? View.GONE : View.VISIBLE);
                    tvBlogsEmptyState.setVisibility(blogs.isEmpty() ? View.VISIBLE : View.GONE);
                    if (blogs.isEmpty()) {
                        tvBlogsEmptyState.setText(R.string.blogs_empty_history);
                    }
                });
    }

    private void confirmDeletePost(BlogPost post) {
        if (post == null || TextUtils.isEmpty(post.getId())) {
            Toast.makeText(this, R.string.blogs_delete_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null || !currentUser.getUid().equals(post.getAuthorUid())) {
            Toast.makeText(this, R.string.blogs_delete_not_owner, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.blogs_delete_title)
                .setMessage(R.string.blogs_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.blogs_delete_confirm, (dialog, which) ->
                        firestore.collection(FirestoreCollections.BLOGS)
                                .document(post.getId())
                                .delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, R.string.blogs_delete_success, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, getString(R.string.blogs_delete_failed, e.getMessage()), Toast.LENGTH_LONG).show()))
                .show();
    }

    private void openAuthorProfile(BlogPost post) {
        if (post == null || TextUtils.isEmpty(post.getAuthorUid())) {
            return;
        }
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", post.getAuthorUid());
        startActivity(intent);
    }

    private void clearErrors() {
        tilBlogTitle.setError(null);
        tilBlogBody.setError(null);
    }

    private void clearInputFields() {
        etBlogTitle.setText("");
        etBlogBody.setText("");
    }

    private void setPublishing(boolean isPublishing) {
        btnPublishBlog.setEnabled(!isPublishing);
    }

    private String valueOf(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private boolean isServerAccessBlocked() {
        return ServerFeatureGate.isServerFeatureBlocked(this);
    }
}
