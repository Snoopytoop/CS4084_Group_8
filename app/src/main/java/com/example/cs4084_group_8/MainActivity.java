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
    private TextInputLayout tilEmail;
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnRegister;
    private TextView tvTitle;
    private TextView tvSwitchMode;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private boolean isRegisterMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firebase initialized. projectId=" + FirebaseApp.getInstance().getOptions().getProjectId());

        btnRegister.setOnClickListener(v -> handleAuthAction());
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail() : "";
            upsertUserProfileAndRoute(user, email, false);
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
        tvTitle = findViewById(R.id.tvTitle);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
    }

    private void updateAuthModeUi() {
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
        Log.d(TAG, "Attempting registration for email: " + email);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auth registration successful for: " + email);
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
                                    userProfile.put("profileCompleted", false);
                                    userProfile.put("createdAt", FieldValue.serverTimestamp());
                                    userProfile.put("updatedAt", FieldValue.serverTimestamp());

                                    firebaseFirestore.collection("users")
                                            .document(currentUser.getUid())
                                            .set(userProfile, SetOptions.merge())
                                            .addOnSuccessListener(unused -> {
                                                Log.d(TAG, "Firestore profile write success for uid=" + currentUser.getUid());
                                                setLoading(false);
                                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                                navigateToProfileSetup();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Firestore profile write failed for uid=" + currentUser.getUid(), e);
                                                setLoading(false);
                                                Toast.makeText(this, "Profile save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                });
                    } else {
                        Log.e(TAG, "Auth registration failed", task.getException());
                        setLoading(false);
                        Toast.makeText(this, "Auth failed: " + readableError(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginWithEmail(String email, String password) {
        Log.d(TAG, "Attempting login for email: " + email);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auth login successful for: " + email);
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if (currentUser == null) {
                            setLoading(false);
                            Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        upsertUserProfileAndRoute(currentUser, email, true);
                    } else {
                        Log.e(TAG, "Auth login failed", task.getException());
                        setLoading(false);
                        Toast.makeText(this, "Login failed: " + readableError(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void upsertUserProfileAndRoute(FirebaseUser currentUser, String email, boolean showLoginToast) {
        String fallbackUsername = currentUser.getDisplayName();
        if (TextUtils.isEmpty(fallbackUsername)) {
            int atIndex = email.indexOf("@");
            fallbackUsername = atIndex > 0 ? email.substring(0, atIndex) : "climber";
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", currentUser.getUid());
        userProfile.put("email", email);
        userProfile.put("username", fallbackUsername);
        userProfile.put("updatedAt", FieldValue.serverTimestamp());

        firebaseFirestore.collection("users")
                .document(currentUser.getUid())
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Firestore profile upsert success for uid=" + currentUser.getUid());
                    firebaseFirestore.collection("users")
                            .document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                Boolean profileCompleted = snapshot.getBoolean("profileCompleted");
                                if (showLoginToast) {
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                }
                                setLoading(false);
                                if (Boolean.TRUE.equals(profileCompleted)) {
                                    navigateToHome();
                                } else {
                                    navigateToProfileSetup();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Firestore profile read failed for uid=" + currentUser.getUid(), e);
                                setLoading(false);
                                navigateToProfileSetup();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore profile upsert failed for uid=" + currentUser.getUid(), e);
                    setLoading(false);
                    Toast.makeText(this, "Profile sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    navigateToProfileSetup();
                });
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        tvSwitchMode.setEnabled(!isLoading);
        // You might want to add a ProgressBar here in your layout
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
        if (exception == null) return "Unknown error";
        String message = exception.getMessage();
        if (message == null) return "Something went wrong";
        
        if (message.contains("Unable to resolve host")) {
            return "No internet connection. Please check your network.";
        }
        return message;
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void navigateToProfileSetup() {
        startActivity(new Intent(this, ProfileSetupActivity.class));
        finish();
    }
}