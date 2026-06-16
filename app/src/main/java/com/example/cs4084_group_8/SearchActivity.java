package com.example.cs4084_group_8;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private TextView tvNoResults;
    private ShapeableImageView ivNavProfile;

    private FirebaseFirestore firestore;
    private UserSearchAdapter adapter;
    private final List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (!ServerFeatureGate.ensureServerFeatureAvailable(this)) {
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        TextInputEditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvSearchResults = findViewById(R.id.rvSearchResults);
        tvNoResults = findViewById(R.id.tvNoResults);
        
        setupNavigation();

        View bottomNav = findViewById(R.id.bottomNavCard);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = 16 + bottomInset;
                v.setLayoutParams(params);
                return insets;
            });
        }

        adapter = new UserSearchAdapter(userList, user -> {
            Intent intent = new Intent(SearchActivity.this, UserProfileActivity.class);
            intent.putExtra("USER_ID", user.getUid());
            startActivity(intent);
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupNavigation() {
        ImageButton btnNavHome = findViewById(R.id.btnNavHome);
        ImageButton btnNavMessages = findViewById(R.id.btnNavMessages);
        ImageButton btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                overridePendingTransition(0, 0);
            });
        }

        if (btnNavMessages != null) {
            btnNavMessages.setOnClickListener(v -> {
                startActivity(new Intent(this, InboxActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (btnNavCreatePost != null) {
            btnNavCreatePost.setOnClickListener(v -> {
                startActivity(new Intent(this, CreatePostActivity.class));
                finish();
                overridePendingTransition(0, 0);
            });
        }

        if (ivNavProfile != null) {
            ivNavProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
                overridePendingTransition(0, 0);
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadNavProfileImage(user.getUid());
        }
    }

    private void searchUsers(String queryText) {
        if (TextUtils.isEmpty(queryText)) {
            userList.clear();
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(View.GONE);
            return;
        }

        firestore.collection("users")
                .orderBy("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().forEach(doc -> {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setUid(doc.getId());
                                userList.add(user);
                            }
                        });
                        tvNoResults.setVisibility(View.GONE);
                    } else {
                        tvNoResults.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.w("SearchActivity", "searchUsers failed", e);
                    userList.clear();
                    adapter.notifyDataSetChanged();
                    tvNoResults.setVisibility(View.VISIBLE);
                    Toast.makeText(SearchActivity.this, R.string.search_failed_toast, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadNavProfileImage(String uid) {
        if (ivNavProfile == null) return;
        firestore.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            String url = snapshot.getString("profileImageUrl");
            if (!TextUtils.isEmpty(url)) {
                Glide.with(this).load(url).placeholder(R.drawable.ic_person).into(ivNavProfile);
            } else {
                ivNavProfile.setImageResource(R.drawable.ic_person);
            }
        });
    }

    private interface OnUserClickListener {
        void onUserClick(User user);
    }

    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<User> users;
        private final OnUserClickListener listener;

        UserSearchAdapter(List<User> users, OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
            ViewHolder vh = new ViewHolder(view);
            vh.text1.setTextColor(ContextCompat.getColor(context, R.color.route_log_text_primary));
            view.setPadding(48, 32, 48, 32);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.text1.setText(user.getUsername());
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    public static class User {
        private String uid;
        private String username;
        private String email;
        private String profileImageUrl;

        public User() {}

        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }
}
