package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityAuth";
    private static final String USERS_COLLECTION = "users";
    private TextInputLayout tilEmail;
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnRegister;
    private MaterialButton btnRootLogin;
    private TextView tvTitle;
    private TextView tvSwitchMode;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private boolean firebaseEnabled;
    private boolean isRegisterMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        firebaseEnabled = !FirebaseApp.getApps(this).isEmpty();
        if (firebaseEnabled) {
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseFirestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase projectId=" + FirebaseApp.getInstance().getOptions().getProjectId());
        } else {
            Log.w(TAG, "Firebase is not configured for this build.");
            setUserAuthEnabled(false);
            Toast.makeText(this, R.string.member_auth_unavailable_toast, Toast.LENGTH_LONG).show();
        }

        btnRegister.setOnClickListener(v -> handleAuthAction());
        btnRootLogin.setOnClickListener(v -> startActivity(new Intent(this, AdminLoginActivity.class)));
        tvSwitchMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            updateAuthModeUi();
            clearInputErrors();
        });

        updateAuthModeUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!firebaseEnabled) {
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            handleExistingSignedInUser(user);
        }
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnRootLogin = findViewById(R.id.btnRootLogin);
        tvTitle = findViewById(R.id.tvTitle);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
    }

    private void updateAuthModeUi() {
        if (!firebaseEnabled) {
            tvTitle.setText(R.string.member_auth_unavailable_title);
            btnRegister.setText(R.string.member_auth_unavailable_button);
            tvSwitchMode.setText(R.string.member_auth_unavailable_message);
            tilUsername.setVisibility(View.GONE);
            return;
        }

        if (isRegisterMode) {
            tvTitle.setText("Create Account");
            btnRegister.setText("Register");
            tvSwitchMode.setText("Already have an account? Login");
            tilUsername.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Welcome Back");
            btnRegister.setText("Login");
            tvSwitchMode.setText("New here? Create account");
            tilUsername.setVisibility(View.GONE);
        }
    }

    private void handleAuthAction() {
        if (!firebaseEnabled) {
            Toast.makeText(this, R.string.member_auth_unavailable_toast, Toast.LENGTH_LONG).show();
            return;
        }

        clearInputErrors();

        String email = valueOf(etEmail);
        String username = valueOf(etUsername);
        String password = valueOf(etPassword);

        boolean hasError = false;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            hasError = true;
        }
        if (isRegisterMode && TextUtils.isEmpty(username)) {
            tilUsername.setError("Username is required");
            hasError = true;
        }
        if (hasError) {
            return;
        }

        setLoading(true);
        if (isRegisterMode) {
            registerWithEmail(email, password, username);
        } else {
            loginWithEmail(email, password);
        }
    }

    private void registerWithEmail(String email, String password, String username) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(this, readableError(task.getException()), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser == null) {
                        setLoading(false);
                        Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build();

                    currentUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                Map<String, Object> userProfile = new HashMap<>();
                                userProfile.put("uid", currentUser.getUid());
                                userProfile.put("email", email);
                                userProfile.put("username", username);
                                userProfile.put(AuthRoles.FIELD_ROLE, AuthRoles.USER);
                                userProfile.put("createdAt", FieldValue.serverTimestamp());
                                userProfile.put("updatedAt", FieldValue.serverTimestamp());

                                firebaseFirestore.collection(USERS_COLLECTION)
                                        .document(currentUser.getUid())
                                        .set(userProfile, SetOptions.merge())
                                        .addOnSuccessListener(unused -> {
                                            Log.d(TAG, "Profile write success for uid=" + currentUser.getUid());
                                            setLoading(false);
                                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                            navigateToHome();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Profile write failed on register uid=" + currentUser.getUid(), e);
                                            setLoading(false);
                                            Toast.makeText(this, "Profile save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            });
                });
    }

    private void loginWithEmail(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if (currentUser == null) {
                            Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        validateAndNavigateMemberLogin(currentUser, email);
                    } else {
                        Toast.makeText(this, readableError(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void validateAndNavigateMemberLogin(FirebaseUser currentUser, String email) {
        firebaseFirestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString(AuthRoles.FIELD_ROLE);
                    if (AuthRoles.ADMIN.equalsIgnoreCase(role)) {
                        setLoading(false);
                        firebaseAuth.signOut();
                        Toast.makeText(this, R.string.admin_use_admin_login_toast, Toast.LENGTH_LONG).show();
                        return;
                    }
                    syncMemberProfileAndNavigate(currentUser, email, true, true);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Unable to verify account role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleExistingSignedInUser(FirebaseUser currentUser) {
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        firebaseFirestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString(AuthRoles.FIELD_ROLE);
                    if (AuthRoles.ADMIN.equalsIgnoreCase(role)) {
                        navigateToAdminDashboard();
                        return;
                    }
                    syncMemberProfileAndNavigate(currentUser, email, false, false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Role lookup failed for uid=" + currentUser.getUid(), e);
                });
    }

    private void syncMemberProfileAndNavigate(
            FirebaseUser currentUser,
            String email,
            boolean showSuccessToast,
            boolean shouldClearLoading
    ) {
        String fallbackUsername = buildFallbackUsername(currentUser, email);

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", currentUser.getUid());
        userProfile.put("email", email);
        userProfile.put("username", fallbackUsername);
        userProfile.put(AuthRoles.FIELD_ROLE, AuthRoles.USER);
        userProfile.put("updatedAt", FieldValue.serverTimestamp());

        firebaseFirestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Member profile sync success uid=" + currentUser.getUid());
                    if (shouldClearLoading) {
                        setLoading(false);
                    }
                    if (showSuccessToast) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                    }
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Member profile sync failed uid=" + currentUser.getUid(), e);
                    if (shouldClearLoading) {
                        setLoading(false);
                    }
                    Toast.makeText(this, "Profile sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    navigateToHome();
                });
    }

    private String buildFallbackUsername(FirebaseUser currentUser, String email) {
        String fallbackUsername = currentUser.getDisplayName();
        if (TextUtils.isEmpty(fallbackUsername)) {
            int atIndex = email.indexOf("@");
            fallbackUsername = atIndex > 0 ? email.substring(0, atIndex) : "climber";
        }
        return fallbackUsername;
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading && firebaseEnabled);
        btnRootLogin.setEnabled(!isLoading);
        tvSwitchMode.setEnabled(!isLoading && firebaseEnabled);
    }

    private void setUserAuthEnabled(boolean isEnabled) {
        tilEmail.setEnabled(isEnabled);
        tilUsername.setEnabled(isEnabled);
        tilPassword.setEnabled(isEnabled);
        etEmail.setEnabled(isEnabled);
        etUsername.setEnabled(isEnabled);
        etPassword.setEnabled(isEnabled);
        btnRegister.setEnabled(isEnabled);
        tvSwitchMode.setEnabled(isEnabled);
    }

    private void clearInputErrors() {
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
    }

    private String valueOf(TextInputEditText inputEditText) {
        if (inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }

    private String readableError(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "Something went wrong. Please try again.";
        }
        return exception.getMessage();
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void navigateToAdminDashboard() {
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }
}
