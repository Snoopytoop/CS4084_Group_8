package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    private static final String PROFILE_CACHE_PREF = "profile_cache";
    private static final String PROFILE_IMAGE_URL_PREFIX = "profile_image_url_";

    private ShapeableImageView ivNavProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        btnNavHome.setOnClickListener(v -> {
            // Already on home.
        });
        ivNavProfile.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadNavProfileImage(user.getUid(), ivNavProfile);
        } else {
            ivNavProfile.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    private void loadNavProfileImage(String uid, ImageView targetView) {
        String cachedUrl = getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                .getString(PROFILE_IMAGE_URL_PREFIX + uid, null);
        if (!TextUtils.isEmpty(cachedUrl)) {
            renderProfileImage(cachedUrl, targetView);
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String imageUrl = snapshot.getString("profileImageUrl");
                    if (TextUtils.isEmpty(imageUrl)) {
                        targetView.setImageResource(android.R.drawable.ic_menu_camera);
                        return;
                    }
                    getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                            .edit()
                            .putString(PROFILE_IMAGE_URL_PREFIX + uid, imageUrl)
                            .apply();
                    renderProfileImage(imageUrl, targetView);
                })
                .addOnFailureListener(e -> targetView.setImageResource(android.R.drawable.ic_menu_camera));
    }

    private void renderProfileImage(String imageUrl, ImageView targetView) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(targetView);
    }
}
