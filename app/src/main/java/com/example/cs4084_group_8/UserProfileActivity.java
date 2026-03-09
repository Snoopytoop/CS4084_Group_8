package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {
    private static final int MENU_EDIT_PROFILE_ID = 1001;
    private static final int MENU_LOGOUT_ID = 1002;
    private static final String PROFILE_CACHE_PREF = "profile_cache";
    private static final String PROFILE_IMAGE_URL_PREFIX = "profile_image_url_";

    private ImageView ivProfile;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvBio;
    private ImageButton btnSettings;
    private ImageButton btnNavHome;
    private ShapeableImageView ivNavProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ivProfile = findViewById(R.id.ivProfile);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);
        btnSettings = findViewById(R.id.btnSettings);
        btnNavHome = findViewById(R.id.btnNavHome);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        btnSettings.setOnClickListener(v -> showSettingsMenu());
        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        ivNavProfile.setOnClickListener(v -> {
            // Already on profile.
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadCurrentUserProfile();
    }

    private void loadCurrentUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        tvEmail.setText(user.getEmail() == null ? "" : user.getEmail());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    String bio = snapshot.getString("bio");
                    String imageUrl = snapshot.getString("profileImageUrl");

                    tvUsername.setText(TextUtils.isEmpty(username) ? "No username yet" : username);
                    tvBio.setText(TextUtils.isEmpty(bio) ? "No bio yet" : bio);

                    if (!TextUtils.isEmpty(imageUrl)) {
                        loadProfileImages(imageUrl);
                    } else {
                        ivProfile.setImageResource(android.R.drawable.ic_menu_camera);
                        ivNavProfile.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadProfileImages(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            getSharedPreferences(PROFILE_CACHE_PREF, MODE_PRIVATE)
                    .edit()
                    .putString(PROFILE_IMAGE_URL_PREFIX + user.getUid(), imageUrl)
                    .apply();
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(ivProfile);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(ivNavProfile);
    }

    private void showSettingsMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnSettings);
        popupMenu.getMenu().add(0, MENU_EDIT_PROFILE_ID, 0, "Edit profile");
        popupMenu.getMenu().add(0, MENU_LOGOUT_ID, 1, "Logout");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_EDIT_PROFILE_ID) {
                Intent intent = new Intent(this, ProfileSetupActivity.class);
                intent.putExtra("fromProfile", true);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == MENU_LOGOUT_ID) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
