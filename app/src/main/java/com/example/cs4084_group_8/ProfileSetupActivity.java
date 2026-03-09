package com.example.cs4084_group_8;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final String TAG = "ProfileSetupActivity";

    private ImageView ivProfile;
    private TextInputEditText etBio;
    private MaterialButton btnUploadImage;
    private MaterialButton btnSkipImage;
    private MaterialButton btnSaveProfile;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;

    private Uri selectedImageUri;
    private String existingImageUrl;
    private boolean isSaving = false;
    private boolean launchedFromProfile = false;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfile.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuth();
            return;
        }

        launchedFromProfile = getIntent().getBooleanExtra("fromProfile", false);
        bindViews();
        loadExistingProfile(currentUser);

        btnUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSkipImage.setOnClickListener(v -> saveProfileWithoutImage(currentUser));
        btnSaveProfile.setOnClickListener(v -> saveProfile(currentUser));
    }

    private void bindViews() {
        ivProfile = findViewById(R.id.ivProfile);
        etBio = findViewById(R.id.etBio);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnSkipImage = findViewById(R.id.btnSkipImage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void saveProfile(FirebaseUser currentUser) {
        if (isSaving) {
            return;
        }

        String bio = etBio.getText() == null ? "" : etBio.getText().toString().trim();

        if (TextUtils.isEmpty(bio)) {
            Toast.makeText(this, "Please write a bio", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        if (selectedImageUri == null) {
            persistProfile(currentUser, bio, existingImageUrl);
            return;
        }

        StorageReference imageRef = firebaseStorage.getReference()
                .child("profile_images")
                .child(currentUser.getUid() + ".jpg");
        Log.d(
                TAG,
                "Uploading profile image to bucket=" + imageRef.getBucket()
                        + ", path=" + imageRef.getPath()
        );

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri ->
                                        persistProfile(currentUser, bio, downloadUri.toString()))
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Download URL lookup failed, saving without new image", e);
                                    Toast.makeText(
                                            this,
                                            "Image uploaded but URL lookup failed: " + readableStorageError(e),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    persistProfile(currentUser, bio, existingImageUrl);
                                }))
                .addOnFailureListener(e -> {
                    Log.w(
                            TAG,
                            "Image upload failed for bucket=" + imageRef.getBucket()
                                    + ", path=" + imageRef.getPath()
                                    + ", uri=" + selectedImageUri
                                    + ". Saving without new image.",
                            e
                    );
                    Toast.makeText(
                            this,
                            "Image upload failed: " + readableStorageError(e)
                                    + ". Saving profile without profile picture.",
                            Toast.LENGTH_LONG
                    ).show();
                    persistProfile(currentUser, bio, existingImageUrl);
                });
    }

    private void saveProfileWithoutImage(FirebaseUser currentUser) {
        if (isSaving) {
            return;
        }
        selectedImageUri = null;
        ivProfile.setImageResource(android.R.drawable.ic_menu_camera);

        String bio = etBio.getText() == null ? "" : etBio.getText().toString().trim();
        if (TextUtils.isEmpty(bio)) {
            Toast.makeText(this, "Please write a bio before skipping the picture", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        persistProfile(currentUser, bio, existingImageUrl);
    }

    private void persistProfile(FirebaseUser currentUser, String bio, String profileImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", bio);
        updates.put("profileImageUrl", profileImageUrl);
        updates.put("profileCompleted", true);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firebaseFirestore.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                    if (launchedFromProfile) {
                        navigateToProfileView();
                    } else {
                        navigateToHome();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadExistingProfile(FirebaseUser currentUser) {
        firebaseFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String bio = snapshot.getString("bio");
                    existingImageUrl = snapshot.getString("profileImageUrl");

                    if (!TextUtils.isEmpty(bio)) {
                        etBio.setText(bio);
                    }
                    if (!TextUtils.isEmpty(existingImageUrl)) {
                        loadImageFromStorageUrl(existingImageUrl);
                    } else {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                });
    }

    private void loadImageFromStorageUrl(String imageUrl) {
        firebaseStorage.getReferenceFromUrl(imageUrl)
                .getBytes(MAX_IMAGE_BYTES)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivProfile.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> ivProfile.setImageResource(android.R.drawable.ic_menu_camera));
    }

    private void setLoading(boolean loading) {
        isSaving = loading;
        btnUploadImage.setEnabled(!loading);
        btnSkipImage.setEnabled(!loading);
        btnSaveProfile.setEnabled(!loading);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void navigateToAuth() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToProfileView() {
        startActivity(new Intent(this, UserProfileActivity.class));
        finish();
    }

    private String readableStorageError(Exception exception) {
        if (exception instanceof StorageException) {
            StorageException storageException = (StorageException) exception;
            return "code=" + storageException.getErrorCode() + ", " + storageException.getMessage();
        }
        if (exception != null && exception.getMessage() != null) {
            return exception.getMessage();
        }
        return "unknown storage error";
    }
}
