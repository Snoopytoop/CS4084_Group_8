package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String USERS_COLLECTION = "users";

    private TextView tvAdminIdentity;
    private TextView tvAdminRole;
    private MaterialButton btnAdminManageUsers;
    private MaterialButton btnAdminModeration;
    private MaterialButton btnAdminSignOut;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private boolean firebaseEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bindViews();
        configureToolbar();

        firebaseEnabled = !FirebaseApp.getApps(this).isEmpty();
        if (firebaseEnabled) {
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseFirestore = FirebaseFirestore.getInstance();
        } else {
            Toast.makeText(this, R.string.admin_dashboard_requires_firebase, Toast.LENGTH_LONG).show();
            returnToSignIn();
            return;
        }

        btnAdminManageUsers.setOnClickListener(view ->
                Toast.makeText(this, R.string.admin_dashboard_user_mgmt_stub, Toast.LENGTH_SHORT).show()
        );
        btnAdminModeration.setOnClickListener(view ->
                Toast.makeText(this, R.string.admin_dashboard_moderation_stub, Toast.LENGTH_SHORT).show()
        );
        btnAdminSignOut.setOnClickListener(view -> signOut());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!firebaseEnabled) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            returnToSignIn();
            return;
        }

        verifyAdminRole(currentUser);
    }

    private void bindViews() {
        tvAdminIdentity = findViewById(R.id.tvAdminIdentity);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        btnAdminManageUsers = findViewById(R.id.btnAdminManageUsers);
        btnAdminModeration = findViewById(R.id.btnAdminModeration);
        btnAdminSignOut = findViewById(R.id.btnAdminSignOut);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAdminDashboard);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void verifyAdminRole(FirebaseUser currentUser) {
        firebaseFirestore.collection(USERS_COLLECTION)
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
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    Toast.makeText(this, getString(R.string.admin_dashboard_role_verify_error, e.getMessage()), Toast.LENGTH_LONG).show();
                    returnToSignIn();
                });
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
