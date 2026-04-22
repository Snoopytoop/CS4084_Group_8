package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private TextInputEditText etSearch;
    private RecyclerView rvSearchResults;
    private TextView tvNoResults;
    private ShapeableImageView ivNavProfile;

    private FirebaseFirestore firestore;
    private UserSearchAdapter adapter;
    private List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (!ServerFeatureGate.ensureServerFeatureAvailable(this)) {
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvNoResults = findViewById(R.id.tvNoResults);
        
        setupNavigation();

        View bottomNav = findViewById(R.id.bottomNavCard);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = 16 + bottomInset;
            v.setLayoutParams(params);
            return insets;
        });

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
        ImageButton btnNavSearch = findViewById(R.id.btnNavSearch);
        ImageButton btnNavLeaderboard = findViewById(R.id.btnNavLeaderboard);
        ImageButton btnNavCreatePost = findViewById(R.id.btnNavCreatePost);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            overridePendingTransition(0, 0);
        });

        btnNavSearch.setOnClickListener(v -> {
            // Already here
        });

        btnNavLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            finish();
            overridePendingTransition(0, 0);
        });

        btnNavCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
            finish();
            overridePendingTransition(0, 0);
        });

        ivNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
            finish();
            overridePendingTransition(0, 0);
        });

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

        // Firestore simple search: prefix search using \uf8ff
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
                                user.setUid(doc.getId()); // Ensure UID is set from document ID
                                userList.add(user);
                            }
                        });
                        tvNoResults.setVisibility(View.GONE);
                    } else {
                        tvNoResults.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadNavProfileImage(String uid) {
        firestore.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            String url = snapshot.getString("profileImageUrl");
            if (!TextUtils.isEmpty(url)) {
                Glide.with(this).load(url).placeholder(android.R.drawable.ic_menu_camera).into(ivNavProfile);
            }
        });
    }

    // Inner Adapter Class
    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<User> users;
        private final OnUserClickListener listener;

        interface OnUserClickListener {
            void onUserClick(User user);
        }

        UserSearchAdapter(List<User> users, OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.text1.setText(user.getUsername());
            holder.text2.setText(user.getEmail());
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // Temporary User class if not already defined (or matches Firestore)
    public static class User {
        private String uid;
        private String username;
        private String email;
        private String profileImageUrl;

        public User() {}

        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getProfileImageUrl() { return profileImageUrl; }
    }
}
