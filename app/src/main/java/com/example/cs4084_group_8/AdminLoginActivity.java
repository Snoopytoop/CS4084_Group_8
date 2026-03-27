package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {
    private static final String USERS_COLLECTION = "users";

    private TextInputLayout tilAdminEmail;
    private TextInputLayout tilAdminPassword;
    private TextInputEditText etAdminEmail;
    private TextInputEditText etAdminPassword;
    private MaterialButton btnAdminLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private boolean firebaseEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        bindViews();
        configureToolbar();

        firebaseEnabled = !FirebaseApp.getApps(this).isEmpty();
        if (firebaseEnabled) {
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseFirestore = FirebaseFirestore.getInstance();
        } else {
            setInputsEnabled(false);
            Toast.makeText(this, R.string.admin_login_requires_firebase, Toast.LENGTH_LONG).show();
        }

        btnAdminLogin.setOnClickListener(view -> attemptAdminLogin());
    }

    private void bindViews() {
        tilAdminEmail = findViewById(R.id.tilAdminEmail);
        tilAdminPassword = findViewById(R.id.tilAdminPassword);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAdminLogin);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void attemptAdminLogin() {
        if (!firebaseEnabled) {
            Toast.makeText(this, R.string.admin_login_requires_firebase, Toast.LENGTH_LONG).show();
            return;
        }

        clearInputErrors();
        String email = valueOf(etAdminEmail);
        String password = valueOf(etAdminPassword);

        boolean hasError = false;
        if (TextUtils.isEmpty(email)) {
            tilAdminEmail.setError(getString(R.string.admin_login_email_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            tilAdminPassword.setError(getString(R.string.admin_login_password_required));
            hasError = true;
        }
        if (hasError) {
            return;
        }

        setInputsEnabled(false);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setInputsEnabled(true);
                        String failureMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.admin_login_failed);
                        Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser == null) {
                        setInputsEnabled(true);
                        Toast.makeText(this, R.string.admin_login_failed, Toast.LENGTH_LONG).show();
                        return;
                    }

                    verifyAdminRoleAndContinue(currentUser);
                });
    }

    private void verifyAdminRoleAndContinue(FirebaseUser currentUser) {
        firebaseFirestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    setInputsEnabled(true);
                    String role = snapshot.getString(AuthRoles.FIELD_ROLE);
                    if (!AuthRoles.ADMIN.equalsIgnoreCase(role)) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, R.string.admin_login_non_admin_error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this, R.string.admin_login_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, AdminDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setInputsEnabled(true);
                    firebaseAuth.signOut();
                    Toast.makeText(this, getString(R.string.admin_login_role_verify_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void clearInputErrors() {
        tilAdminEmail.setError(null);
        tilAdminPassword.setError(null);
    }

    private void setInputsEnabled(boolean enabled) {
        tilAdminEmail.setEnabled(enabled);
        tilAdminPassword.setEnabled(enabled);
        etAdminEmail.setEnabled(enabled);
        etAdminPassword.setEnabled(enabled);
        btnAdminLogin.setEnabled(enabled);
    }

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }
}
