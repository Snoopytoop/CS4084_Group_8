package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminDashboardActivity extends AppCompatActivity {
    private TextView tvAdminIdentity;
    private TextView tvAdminRole;
    private TextView tvUsersCount;
    private TextView tvPostsCount;
    private TextView tvRouteLogsCount;
    private TextView tvBelayerPostsCount;
    private MaterialButton btnAdminSignOut;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ListenerRegistration usersCountListener;
    private ListenerRegistration postsCountListener;
    private ListenerRegistration routeLogsCountListener;
    private ListenerRegistration belayerPostsCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bindViews();
        configureToolbar();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
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
