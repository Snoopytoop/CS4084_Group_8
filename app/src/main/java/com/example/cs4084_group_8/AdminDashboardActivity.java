package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final int RECENT_ACTIVITY_LIMIT = 4;

    private TextView tvAdminIdentity;
    private TextView tvAdminRole;
    private TextView tvUsersCount;
    private TextView tvPostsCount;
    private TextView tvRouteLogsCount;
    private TextView tvBelayerPostsCount;
    private LinearLayout recentUsersContainer;
    private LinearLayout recentPostsContainer;
    private LinearLayout recentRouteLogsContainer;
    private LinearLayout recentBelayerPostsContainer;
    private MaterialButton btnAdminSignOut;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private LayoutInflater layoutInflater;
    private ListenerRegistration usersCountListener;
    private ListenerRegistration postsCountListener;
    private ListenerRegistration routeLogsCountListener;
    private ListenerRegistration belayerPostsCountListener;
    private ListenerRegistration recentUsersListener;
    private ListenerRegistration recentPostsListener;
    private ListenerRegistration recentRouteLogsListener;
    private ListenerRegistration recentBelayerPostsListener;
    private final SimpleDateFormat activityTimestampFormat =
            new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bindViews();
        configureToolbar();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        layoutInflater = LayoutInflater.from(this);
        btnAdminSignOut.setOnClickListener(view -> signOut());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            returnToSignIn();
            return;
        }

        verifyAdminRole(currentUser);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseListeners();
    }

    private void bindViews() {
        tvAdminIdentity = findViewById(R.id.tvAdminIdentity);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvPostsCount = findViewById(R.id.tvPostsCount);
        tvRouteLogsCount = findViewById(R.id.tvRouteLogsCount);
        tvBelayerPostsCount = findViewById(R.id.tvBelayerPostsCount);
        recentUsersContainer = findViewById(R.id.recentUsersContainer);
        recentPostsContainer = findViewById(R.id.recentPostsContainer);
        recentRouteLogsContainer = findViewById(R.id.recentRouteLogsContainer);
        recentBelayerPostsContainer = findViewById(R.id.recentBelayerPostsContainer);
        btnAdminSignOut = findViewById(R.id.btnAdminSignOut);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAdminDashboard);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void verifyAdminRole(FirebaseUser currentUser) {
        firebaseFirestore.collection(FirestoreCollections.USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString(AuthRoles.FIELD_ROLE);
                    if (!AuthRoles.ADMIN.equalsIgnoreCase(role)) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, R.string.admin_dashboard_not_authorized, Toast.LENGTH_LONG).show();
                        returnToSignIn();
                        return;
                    }

                    String email = currentUser.getEmail();
                    if (TextUtils.isEmpty(email)) {
                        email = getString(R.string.admin_dashboard_unknown_account);
                    }

                    tvAdminIdentity.setText(getString(R.string.admin_dashboard_identity_format, email));
                    tvAdminRole.setText(getString(R.string.admin_dashboard_role_format, AuthRoles.ADMIN));
                    bindCollectionCounts();
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    Toast.makeText(this, getString(R.string.admin_dashboard_role_verify_error, e.getMessage()), Toast.LENGTH_LONG).show();
                    returnToSignIn();
                });
    }

    private void bindCollectionCounts() {
        releaseListeners();
        usersCountListener = bindCount(FirestoreCollections.USERS, tvUsersCount);
        postsCountListener = bindCount(FirestoreCollections.POSTS, tvPostsCount);
        routeLogsCountListener = bindCount(FirestoreCollections.ROUTE_LOGS, tvRouteLogsCount);
        belayerPostsCountListener = bindCount(FirestoreCollections.BELAYER_POSTS, tvBelayerPostsCount);
        recentUsersListener = bindRecentUsers();
        recentPostsListener = bindRecentPosts();
        recentRouteLogsListener = bindRecentRouteLogs();
        recentBelayerPostsListener = bindRecentBelayerPosts();
    }

    private ListenerRegistration bindCount(String collectionName, TextView targetView) {
        return firebaseFirestore.collection(collectionName)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        targetView.setText(getString(R.string.admin_dashboard_stat_unavailable));
                        return;
                    }

                    int count = value != null ? value.size() : 0;
                    targetView.setText(String.valueOf(count));
                });
    }

    private ListenerRegistration bindRecentUsers() {
        showSectionMessage(recentUsersContainer, getString(R.string.admin_dashboard_recent_loading));
        return firebaseFirestore.collection(FirestoreCollections.USERS)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(RECENT_ACTIVITY_LIMIT)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showSectionMessage(
                                recentUsersContainer,
                                getString(R.string.admin_dashboard_recent_users_failed, error.getMessage())
                        );
                        return;
                    }

                    recentUsersContainer.removeAllViews();
                    if (value == null || value.isEmpty()) {
                        showSectionMessage(recentUsersContainer, getString(R.string.admin_dashboard_recent_users_empty));
                        return;
                    }

                    value.getDocuments().forEach(snapshot -> {
                        String userId = snapshot.getString("uid");
                        if (TextUtils.isEmpty(userId)) {
                            userId = snapshot.getId();
                        }

                        String email = snapshot.getString("email");
                        String username = snapshot.getString("username");
                        String role = snapshot.getString(AuthRoles.FIELD_ROLE);
                        String title = firstNonEmpty(username, email, getString(R.string.admin_dashboard_recent_unknown_user));
                        String subtitle = TextUtils.isEmpty(email)
                                ? getString(R.string.admin_dashboard_recent_role_format, normalizeRole(role))
                                : email;
                        String meta = formatTimestamp(snapshot.getTimestamp("updatedAt"));
                        if (TextUtils.isEmpty(meta)) {
                            meta = getString(R.string.admin_dashboard_recent_role_format, normalizeRole(role));
                        }

                        recentUsersContainer.addView(createActivityRow(recentUsersContainer, title, subtitle, meta, userId));
                    });
                });
    }

    private ListenerRegistration bindRecentPosts() {
        showSectionMessage(recentPostsContainer, getString(R.string.admin_dashboard_recent_loading));
        return firebaseFirestore.collection(FirestoreCollections.POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(RECENT_ACTIVITY_LIMIT)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showSectionMessage(
                                recentPostsContainer,
                                getString(R.string.admin_dashboard_recent_posts_failed, error.getMessage())
                        );
                        return;
                    }

                    recentPostsContainer.removeAllViews();
                    if (value == null || value.isEmpty()) {
                        showSectionMessage(recentPostsContainer, getString(R.string.admin_dashboard_recent_posts_empty));
                        return;
                    }

                    value.getDocuments().forEach(snapshot -> {
                        Post post = snapshot.toObject(Post.class);
                        if (post == null) {
                            return;
                        }

                        String title = getString(
                                R.string.admin_dashboard_recent_post_title_format,
                                firstNonEmpty(post.getAuthorName(), getString(R.string.admin_dashboard_recent_unknown_user))
                        );
                        String subtitle = firstNonEmpty(
                                summarize(post.getContent()),
                                getString(R.string.admin_dashboard_recent_missing_content)
                        );
                        String meta = formatTimestamp(post.getCreatedAt());
                        if (TextUtils.isEmpty(meta)) {
                            meta = getString(
                                    R.string.admin_dashboard_recent_post_meta_format,
                                    firstNonEmpty(post.getPostType(), getString(R.string.admin_dashboard_recent_unknown_post_type))
                            );
                        }

                        recentPostsContainer.addView(createActivityRow(recentPostsContainer, title, subtitle, meta, post.getAuthorUid()));
                    });
                });
    }

    private ListenerRegistration bindRecentRouteLogs() {
        showSectionMessage(recentRouteLogsContainer, getString(R.string.admin_dashboard_recent_loading));
        return firebaseFirestore.collection(FirestoreCollections.ROUTE_LOGS)
                .orderBy("loggedAt", Query.Direction.DESCENDING)
                .limit(RECENT_ACTIVITY_LIMIT)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showSectionMessage(
                                recentRouteLogsContainer,
                                getString(R.string.admin_dashboard_recent_routes_failed, error.getMessage())
                        );
                        return;
                    }

                    recentRouteLogsContainer.removeAllViews();
                    if (value == null || value.isEmpty()) {
                        showSectionMessage(recentRouteLogsContainer, getString(R.string.admin_dashboard_recent_routes_empty));
                        return;
                    }

                    value.getDocuments().forEach(snapshot -> {
                        RouteLogEntry entry = snapshot.toObject(RouteLogEntry.class);
                        if (entry == null) {
                            return;
                        }

                        String title = getString(
                                R.string.admin_dashboard_recent_route_title_format,
                                firstNonEmpty(entry.getAuthorName(), getString(R.string.admin_dashboard_recent_unknown_user)),
                                firstNonEmpty(entry.getRouteName(), getString(R.string.admin_dashboard_recent_unknown_route))
                        );
                        String subtitle = getString(
                                R.string.admin_dashboard_recent_route_subtitle_format,
                                firstNonEmpty(entry.getGrade(), getString(R.string.route_log_grade_unknown)),
                                entry.getAttempts()
                        );
                        String meta = formatTimestamp(entry.getLoggedAt());

                        recentRouteLogsContainer.addView(createActivityRow(recentRouteLogsContainer, title, subtitle, meta, entry.getAuthorUid()));
                    });
                });
    }

    private ListenerRegistration bindRecentBelayerPosts() {
        showSectionMessage(recentBelayerPostsContainer, getString(R.string.admin_dashboard_recent_loading));
        return firebaseFirestore.collection(FirestoreCollections.BELAYER_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(RECENT_ACTIVITY_LIMIT)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showSectionMessage(
                                recentBelayerPostsContainer,
                                getString(R.string.admin_dashboard_recent_belayers_failed, error.getMessage())
                        );
                        return;
                    }

                    recentBelayerPostsContainer.removeAllViews();
                    if (value == null || value.isEmpty()) {
                        showSectionMessage(
                                recentBelayerPostsContainer,
                                getString(R.string.admin_dashboard_recent_belayers_empty)
                        );
                        return;
                    }

                    value.getDocuments().forEach(snapshot -> {
                        BelayerPost post = snapshot.toObject(BelayerPost.class);
                        if (post == null) {
                            return;
                        }

                        String title = getString(
                                R.string.admin_dashboard_recent_belayer_title_format,
                                firstNonEmpty(post.getAuthorName(), getString(R.string.admin_dashboard_recent_unknown_user)),
                                firstNonEmpty(post.getWallName(), getString(R.string.admin_dashboard_recent_unknown_wall))
                        );
                        String subtitle = getString(
                                R.string.admin_dashboard_recent_belayer_subtitle_format,
                                firstNonEmpty(post.getClimbDays(), getString(R.string.find_belayer_days_hint)),
                                firstNonEmpty(post.getClimbTimes(), getString(R.string.find_belayer_times_hint))
                        );
                        String meta = formatTimestamp(post.getCreatedAt());

                        recentBelayerPostsContainer.addView(createActivityRow(recentBelayerPostsContainer, title, subtitle, meta, post.getAuthorUid()));
                    });
                });
    }

    private View createActivityRow(ViewGroup parent, String title, String subtitle, String meta, String targetUserId) {
        View row = layoutInflater.inflate(R.layout.item_admin_activity, parent, false);
        TextView tvTitle = row.findViewById(R.id.tvAdminActivityTitle);
        TextView tvSubtitle = row.findViewById(R.id.tvAdminActivitySubtitle);
        TextView tvMeta = row.findViewById(R.id.tvAdminActivityMeta);

        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        if (TextUtils.isEmpty(meta)) {
            tvMeta.setVisibility(View.GONE);
        } else {
            tvMeta.setVisibility(View.VISIBLE);
            tvMeta.setText(meta);
        }

        if (!TextUtils.isEmpty(targetUserId)) {
            row.setOnClickListener(view -> openUserProfile(targetUserId));
        } else {
            row.setOnClickListener(null);
            row.setClickable(false);
            row.setFocusable(false);
        }

        return row;
    }

    private void openUserProfile(String userId) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void showSectionMessage(ViewGroup container, String message) {
        container.removeAllViews();
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(0, 8, 0, 0);
        textView.setAlpha(0.78f);
        container.addView(textView);
    }

    private String normalizeRole(String role) {
        return firstNonEmpty(role, getString(R.string.admin_dashboard_recent_unknown_role));
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private String summarize(String value) {
        String trimmed = firstNonEmpty(value).replace('\n', ' ').trim();
        if (trimmed.length() <= 88) {
            return trimmed;
        }
        return trimmed.substring(0, 85).trim() + "...";
    }

    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return activityTimestampFormat.format(timestamp.toDate());
    }

    private void releaseListeners() {
        if (usersCountListener != null) {
            usersCountListener.remove();
            usersCountListener = null;
        }
        if (postsCountListener != null) {
            postsCountListener.remove();
            postsCountListener = null;
        }
        if (routeLogsCountListener != null) {
            routeLogsCountListener.remove();
            routeLogsCountListener = null;
        }
        if (belayerPostsCountListener != null) {
            belayerPostsCountListener.remove();
            belayerPostsCountListener = null;
        }
        if (recentUsersListener != null) {
            recentUsersListener.remove();
            recentUsersListener = null;
        }
        if (recentPostsListener != null) {
            recentPostsListener.remove();
            recentPostsListener = null;
        }
        if (recentRouteLogsListener != null) {
            recentRouteLogsListener.remove();
            recentRouteLogsListener = null;
        }
        if (recentBelayerPostsListener != null) {
            recentBelayerPostsListener.remove();
            recentBelayerPostsListener = null;
        }
    }

    private void signOut() {
        firebaseAuth.signOut();
        returnToSignIn();
    }

    private void returnToSignIn() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
