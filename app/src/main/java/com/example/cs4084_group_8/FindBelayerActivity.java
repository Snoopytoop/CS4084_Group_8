package com.example.cs4084_group_8;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FindBelayerActivity extends AppCompatActivity {
    private TextInputLayout tilBelayerName;
    private TextInputLayout tilBelayerWall;
    private TextInputLayout tilBelayerDays;
    private TextInputLayout tilBelayerTimes;
    private TextInputLayout tilBelayerCapability;
    private TextInputLayout tilClimbingCapability;
    private TextInputLayout tilBelayerNotes;

    private TextInputEditText etBelayerName;
    private TextInputEditText etBelayerWall;
    private TextInputEditText etBelayerDays;
    private TextInputEditText etBelayerTimes;
    private AutoCompleteTextView actvBelayerCapability;
    private TextInputEditText etClimbingCapability;
    private TextInputEditText etBelayerNotes;
    private MaterialButton btnPublishBelayerPost;

    private TextView tvBelayerPostsValue;
    private TextView tvBelayerWallsValue;
    private TextView tvBelayerPostsEmpty;
    private RecyclerView rvBelayerPosts;

    private BelayerBoardStore belayerBoardStore;
    private BelayerPostAdapter belayerPostAdapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_belayer);

        bindViews();
        configureToolbar();
        configureBelayerCapabilityDropdown();
        configureBelayerPostsList();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        prefillDisplayName();

        belayerBoardStore = new BelayerBoardStore(this);
        btnPublishBelayerPost.setOnClickListener(view -> publishBelayerPost());
        loadBelayerPosts();
    }

    private void bindViews() {
        tilBelayerName = findViewById(R.id.tilBelayerName);
        tilBelayerWall = findViewById(R.id.tilBelayerWall);
        tilBelayerDays = findViewById(R.id.tilBelayerDays);
        tilBelayerTimes = findViewById(R.id.tilBelayerTimes);
        tilBelayerCapability = findViewById(R.id.tilBelayerCapability);
        tilClimbingCapability = findViewById(R.id.tilClimbingCapability);
        tilBelayerNotes = findViewById(R.id.tilBelayerNotes);

        etBelayerName = findViewById(R.id.etBelayerName);
        etBelayerWall = findViewById(R.id.etBelayerWall);
        etBelayerDays = findViewById(R.id.etBelayerDays);
        etBelayerTimes = findViewById(R.id.etBelayerTimes);
        actvBelayerCapability = findViewById(R.id.actvBelayerCapability);
        etClimbingCapability = findViewById(R.id.etClimbingCapability);
        etBelayerNotes = findViewById(R.id.etBelayerNotes);
        btnPublishBelayerPost = findViewById(R.id.btnPublishBelayerPost);

        tvBelayerPostsValue = findViewById(R.id.tvBelayerPostsValue);
        tvBelayerWallsValue = findViewById(R.id.tvBelayerWallsValue);
        tvBelayerPostsEmpty = findViewById(R.id.tvBelayerPostsEmpty);
        rvBelayerPosts = findViewById(R.id.rvBelayerPosts);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarFindBelayer);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void configureBelayerCapabilityDropdown() {
        String[] capabilities = getResources().getStringArray(R.array.belay_capability_options);
        ArrayAdapter<String> capabilityAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, capabilities);
        actvBelayerCapability.setAdapter(capabilityAdapter);
        if (capabilities.length > 0) {
            actvBelayerCapability.setText(capabilities[0], false);
        }
    }

    private void configureBelayerPostsList() {
        rvBelayerPosts.setLayoutManager(new LinearLayoutManager(this));
        belayerPostAdapter = new BelayerPostAdapter(getLayoutInflater(), this::onMessageClick);
        rvBelayerPosts.setAdapter(belayerPostAdapter);
    }

    private void prefillDisplayName() {
        if (currentUser == null) {
            return;
        }
        if (!TextUtils.isEmpty(valueOf(etBelayerName))) {
            return;
        }

        String displayName = currentUser.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = currentUser.getEmail();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.find_belayer_default_name);
        }

        etBelayerName.setText(displayName);
    }

    private void publishBelayerPost() {
        clearInputErrors();

        String displayName = valueOf(etBelayerName);
        String wallName = valueOf(etBelayerWall);
        String climbDays = valueOf(etBelayerDays);
        String climbTimes = valueOf(etBelayerTimes);
        String belayCapability = valueOf(actvBelayerCapability);
        String climbCapability = valueOf(etClimbingCapability);
        String notes = valueOf(etBelayerNotes);

        boolean hasError = false;
        if (TextUtils.isEmpty(wallName)) {
            tilBelayerWall.setError(getString(R.string.find_belayer_wall_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbDays)) {
            tilBelayerDays.setError(getString(R.string.find_belayer_days_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbTimes)) {
            tilBelayerTimes.setError(getString(R.string.find_belayer_times_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(belayCapability)) {
            tilBelayerCapability.setError(getString(R.string.find_belayer_belay_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(climbCapability)) {
            tilClimbingCapability.setError(getString(R.string.find_belayer_climb_required));
            hasError = true;
        }
        if (hasError) {
            return;
        }

        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.find_belayer_default_name);
        }

        String contactKey = currentUser != null
                ? currentUser.getUid()
                : displayName.toLowerCase(Locale.getDefault()).replace(" ", "_");

        BelayerPost post = new BelayerPost(
                UUID.randomUUID().toString(),
                displayName,
                wallName,
                climbDays,
                climbTimes,
                belayCapability,
                climbCapability,
                notes,
                contactKey,
                System.currentTimeMillis()
        );
        belayerBoardStore.addPost(post);

        clearPostForm();
        loadBelayerPosts();
        Toast.makeText(this, R.string.find_belayer_saved_toast, Toast.LENGTH_SHORT).show();
    }

    private void onMessageClick(BelayerPost post) {
        // Integration seam: hook this callback into the DM screen when messaging is available.
        Toast.makeText(this, R.string.find_belayer_message_stub_toast, Toast.LENGTH_SHORT).show();
    }

    private void clearInputErrors() {
        tilBelayerName.setError(null);
        tilBelayerWall.setError(null);
        tilBelayerDays.setError(null);
        tilBelayerTimes.setError(null);
        tilBelayerCapability.setError(null);
        tilClimbingCapability.setError(null);
        tilBelayerNotes.setError(null);
    }

    private void clearPostForm() {
        etBelayerWall.setText("");
        etBelayerDays.setText("");
        etBelayerTimes.setText("");
        etClimbingCapability.setText("");
        etBelayerNotes.setText("");

        String[] capabilities = getResources().getStringArray(R.array.belay_capability_options);
        if (capabilities.length > 0) {
            actvBelayerCapability.setText(capabilities[0], false);
        } else {
            actvBelayerCapability.setText("");
        }
    }

    private void loadBelayerPosts() {
        List<BelayerPost> posts = belayerBoardStore.getAllPosts();
        belayerPostAdapter.submitPosts(posts);
        updateSummary(posts);

        boolean hasPosts = !posts.isEmpty();
        rvBelayerPosts.setVisibility(hasPosts ? View.VISIBLE : View.GONE);
        tvBelayerPostsEmpty.setVisibility(hasPosts ? View.GONE : View.VISIBLE);
    }

    private void updateSummary(List<BelayerPost> posts) {
        Set<String> uniqueWalls = new HashSet<>();
        for (BelayerPost post : posts) {
            uniqueWalls.add(post.getWallName().trim().toLowerCase(Locale.getDefault()));
        }

        tvBelayerPostsValue.setText(getString(R.string.find_belayer_total_posts_value, posts.size()));
        tvBelayerWallsValue.setText(getString(R.string.find_belayer_total_walls_value, uniqueWalls.size()));
    }

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }
}
