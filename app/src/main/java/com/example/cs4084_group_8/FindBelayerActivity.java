package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FindBelayerActivity extends AppCompatActivity {
    private TextInputLayout tilBelayerName;
    private TextInputLayout tilBelayerWall;
    private TextInputLayout tilBelayerCapability;
    private TextInputLayout tilClimbingCapability;
    private TextInputLayout tilBelayerNotes;

    private TextInputEditText etBelayerName;
    private TextInputEditText etBelayerWall;
    private AutoCompleteTextView actvBelayerCapability;
    private TextInputEditText etClimbingCapability;
    private TextInputEditText etBelayerNotes;
    
    private MaterialButton btnSelectDays;
    private MaterialButton btnSelectTimes;
    private TextView tvSelectedDays;
    private TextView tvSelectedTimes;
    private MaterialButton btnPublishBelayerPost;
    
    private MaterialButton btnFilterDays;
    private MaterialButton btnFilterTimes;
    private MaterialButton btnApplyFilters;
    private MaterialButton btnClearFilters;
    private TextView tvFilterStatus;

    private TextView tvBelayerPostsValue;
    private TextView tvBelayerWallsValue;
    private TextView tvBelayerPostsEmpty;
    private RecyclerView rvBelayerPosts;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private BelayerPostAdapter belayerPostAdapter;
    private ListenerRegistration belayerPostsListener;
    
    // Selected days and times for post creation
    private Set<Integer> selectedDayIndices = new HashSet<>();
    private Set<Integer> selectedTimeIndices = new HashSet<>();
    
    // Selected filters
    private Set<Integer> filterDayIndices = new HashSet<>();
    private Set<Integer> filterTimeIndices = new HashSet<>();
    private List<BelayerPost> allPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_belayer);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        bindViews();
        configureToolbar();
        configureBelayerCapabilityDropdown();
        configureBelayerPostsList();
        configureDayAndTimeSelectors();
        configureFilterButtons();
        prefillDisplayName();

        btnPublishBelayerPost.setOnClickListener(view -> publishBelayerPost());
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        listenForBelayerPosts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (belayerPostsListener != null) {
            belayerPostsListener.remove();
            belayerPostsListener = null;
        }
    }

    private void bindViews() {
        tilBelayerName = findViewById(R.id.tilBelayerName);
        tilBelayerWall = findViewById(R.id.tilBelayerWall);
        tilBelayerCapability = findViewById(R.id.tilBelayerCapability);
        tilClimbingCapability = findViewById(R.id.tilClimbingCapability);
        tilBelayerNotes = findViewById(R.id.tilBelayerNotes);

        etBelayerName = findViewById(R.id.etBelayerName);
        etBelayerWall = findViewById(R.id.etBelayerWall);
        actvBelayerCapability = findViewById(R.id.actvBelayerCapability);
        etClimbingCapability = findViewById(R.id.etClimbingCapability);
        etBelayerNotes = findViewById(R.id.etBelayerNotes);
        
        btnSelectDays = findViewById(R.id.btnSelectDays);
        btnSelectTimes = findViewById(R.id.btnSelectTimes);
        tvSelectedDays = findViewById(R.id.tvSelectedDays);
        tvSelectedTimes = findViewById(R.id.tvSelectedTimes);
        btnPublishBelayerPost = findViewById(R.id.btnPublishBelayerPost);
        
        btnFilterDays = findViewById(R.id.btnFilterDays);
        btnFilterTimes = findViewById(R.id.btnFilterTimes);
        btnApplyFilters = findViewById(R.id.btnApplyFilters);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        tvFilterStatus = findViewById(R.id.tvFilterStatus);

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
        belayerPostAdapter = new BelayerPostAdapter(
                getLayoutInflater(),
                new BelayerPostAdapter.ActionListener() {
                    @Override
                    public void onMessage(BelayerPost post) {
                        openConversation(post);
                    }

                    @Override
                    public void onDeletePost(BelayerPost post) {
                        confirmDeletePost(post);
                    }

                    @Override
                    public void onViewProfile(BelayerPost post) {
                        openProfile(post);
                    }
                },
                currentUser != null ? currentUser.getUid() : ""
        );
        rvBelayerPosts.setAdapter(belayerPostAdapter);
    }

    private void prefillDisplayName() {
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
        if (isServerAccessBlocked()) {
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        clearInputErrors();

        String displayName = valueOf(etBelayerName);
        String wallName = valueOf(etBelayerWall);
        String climbDays = formatSelectedDays();
        String climbTimes = formatSelectedTimes();
        String belayCapability = valueOf(actvBelayerCapability);
        String climbCapability = valueOf(etClimbingCapability);
        String notes = valueOf(etBelayerNotes);

        boolean hasError = false;
        if (TextUtils.isEmpty(wallName)) {
            tilBelayerWall.setError(getString(R.string.find_belayer_wall_required));
            hasError = true;
        }
        if (selectedDayIndices.isEmpty()) {
            Toast.makeText(this, R.string.find_belayer_days_required, Toast.LENGTH_SHORT).show();
            hasError = true;
        }
        if (selectedTimeIndices.isEmpty()) {
            Toast.makeText(this, R.string.find_belayer_times_required, Toast.LENGTH_SHORT).show();
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

        btnPublishBelayerPost.setEnabled(false);
        Map<String, Object> postData = new HashMap<>();
        postData.put("authorUid", currentUser.getUid());
        postData.put("authorName", displayName);
        postData.put("wallName", wallName);
        postData.put("climbDays", climbDays);
        postData.put("climbTimes", climbTimes);
        postData.put("belayCapability", belayCapability);
        postData.put("climbCapability", climbCapability);
        postData.put("notes", notes);
        postData.put("createdAt", FieldValue.serverTimestamp());

        if (isServerAccessBlocked()) {
            btnPublishBelayerPost.setEnabled(true);
            Toast.makeText(this, R.string.home_offline_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .add(postData)
                .addOnSuccessListener(documentReference -> {
                    btnPublishBelayerPost.setEnabled(true);
                    clearPostForm();
                    Toast.makeText(this, R.string.find_belayer_saved_toast, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnPublishBelayerPost.setEnabled(true);
                    Toast.makeText(this, getString(R.string.find_belayer_save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForBelayerPosts() {
        if (belayerPostsListener != null) {
            belayerPostsListener.remove();
        }

        tvBelayerPostsEmpty.setVisibility(TextView.VISIBLE);
        tvBelayerPostsEmpty.setText(R.string.find_belayer_loading_history);
        rvBelayerPosts.setVisibility(RecyclerView.GONE);

        belayerPostsListener = firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        rvBelayerPosts.setVisibility(RecyclerView.GONE);
                        tvBelayerPostsEmpty.setVisibility(TextView.VISIBLE);
                        tvBelayerPostsEmpty.setText(getString(R.string.find_belayer_load_failed, error.getMessage()));
                        return;
                    }

                    allPosts = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            BelayerPost post = documentSnapshot.toObject(BelayerPost.class);
                            if (post != null) {
                                post.setId(documentSnapshot.getId());
                                allPosts.add(post);
                            }
                        });
                    }
                    
                    allPosts.sort(Comparator.comparing(
                            BelayerPost::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());
                    
                    // Apply filters if any are selected
                    List<BelayerPost> postsToDisplay = applyFilters(allPosts);
                    
                    belayerPostAdapter.submitList(postsToDisplay);
                    updateSummary(postsToDisplay);

                    boolean hasPosts = !postsToDisplay.isEmpty();
                    rvBelayerPosts.setVisibility(hasPosts ? RecyclerView.VISIBLE : RecyclerView.GONE);
                    tvBelayerPostsEmpty.setVisibility(hasPosts ? TextView.GONE : TextView.VISIBLE);
                    if (!hasPosts) {
                        tvBelayerPostsEmpty.setText(R.string.find_belayer_empty_history);
                    }
                });
    }

    private void updateSummary(List<BelayerPost> posts) {
        Set<String> uniqueWalls = new HashSet<>();
        for (BelayerPost post : posts) {
            uniqueWalls.add(post.getWallName().trim().toLowerCase(Locale.getDefault()));
        }

        tvBelayerPostsValue.setText(getString(R.string.find_belayer_total_posts_value, posts.size()));
        tvBelayerWallsValue.setText(getString(R.string.find_belayer_total_walls_value, uniqueWalls.size()));
    }

    private void openConversation(BelayerPost post) {
        if (TextUtils.isEmpty(post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_message_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser != null && TextUtils.equals(currentUser.getUid(), post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_message_self_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, post.getAuthorUid());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, post.getAuthorName());
        startActivity(intent);
    }

    private void openProfile(BelayerPost post) {
        if (TextUtils.isEmpty(post.getAuthorUid())) {
            return;
        }

        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", post.getAuthorUid());
        startActivity(intent);
    }

    private void confirmDeletePost(BelayerPost post) {
        if (TextUtils.isEmpty(post.getId())) {
            Toast.makeText(this, R.string.find_belayer_delete_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null || !TextUtils.equals(currentUser.getUid(), post.getAuthorUid())) {
            Toast.makeText(this, R.string.find_belayer_delete_not_owner, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.find_belayer_delete_title)
                .setMessage(R.string.find_belayer_delete_message)
                .setPositiveButton(R.string.find_belayer_delete_confirm, (dialog, which) -> deletePost(post.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deletePost(String postId) {
        firestore.collection(FirestoreCollections.BELAYER_POSTS)
                .document(postId)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.find_belayer_delete_success, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.find_belayer_delete_failed, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }

    private void clearInputErrors() {
        tilBelayerName.setError(null);
        tilBelayerWall.setError(null);
        tilBelayerCapability.setError(null);
        tilClimbingCapability.setError(null);
        tilBelayerNotes.setError(null);
    }

    private void clearPostForm() {
        etBelayerWall.setText("");
        etClimbingCapability.setText("");
        etBelayerNotes.setText("");
        
        // Clear selected days and times
        selectedDayIndices.clear();
        selectedTimeIndices.clear();
        updateSelectedDaysDisplay();
        updateSelectedTimesDisplay();

        String[] capabilities = getResources().getStringArray(R.array.belay_capability_options);
        if (capabilities.length > 0) {
            actvBelayerCapability.setText(capabilities[0], false);
        } else {
            actvBelayerCapability.setText("");
        }
    }

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }
    
    private void configureDayAndTimeSelectors() {
        btnSelectDays.setOnClickListener(v -> showDaySelectionDialog());
        btnSelectTimes.setOnClickListener(v -> showTimeSelectionDialog());
    }
    
    private void configureFilterButtons() {
        btnFilterDays.setOnClickListener(v -> showFilterDaySelectionDialog());
        btnFilterTimes.setOnClickListener(v -> showFilterTimeSelectionDialog());
        btnApplyFilters.setOnClickListener(v -> applyFiltersAndRefresh());
        btnClearFilters.setOnClickListener(v -> {
            filterDayIndices.clear();
            filterTimeIndices.clear();
            updateFilterStatus();
            applyFiltersAndRefresh();
        });
    }
    
    private void showDaySelectionDialog() {
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        boolean[] checkedItems = new boolean[daysArray.length];
        for (int i = 0; i < daysArray.length; i++) {
            checkedItems[i] = selectedDayIndices.contains(i);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.find_belayer_select_days_title)
                .setMultiChoiceItems(daysArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedDayIndices.add(which);
                    } else {
                        selectedDayIndices.remove(which);
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    updateSelectedDaysDisplay();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void showTimeSelectionDialog() {
        String[] timesArray = getResources().getStringArray(R.array.time_periods);
        boolean[] checkedItems = new boolean[timesArray.length];
        for (int i = 0; i < timesArray.length; i++) {
            checkedItems[i] = selectedTimeIndices.contains(i);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.find_belayer_select_times_title)
                .setMultiChoiceItems(timesArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedTimeIndices.add(which);
                    } else {
                        selectedTimeIndices.remove(which);
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    updateSelectedTimesDisplay();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void showFilterDaySelectionDialog() {
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        boolean[] checkedItems = new boolean[daysArray.length];
        for (int i = 0; i < daysArray.length; i++) {
            checkedItems[i] = filterDayIndices.contains(i);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.find_belayer_select_days_title)
                .setMultiChoiceItems(daysArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        filterDayIndices.add(which);
                    } else {
                        filterDayIndices.remove(which);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void showFilterTimeSelectionDialog() {
        String[] timesArray = getResources().getStringArray(R.array.time_periods);
        boolean[] checkedItems = new boolean[timesArray.length];
        for (int i = 0; i < timesArray.length; i++) {
            checkedItems[i] = filterTimeIndices.contains(i);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.find_belayer_select_times_title)
                .setMultiChoiceItems(timesArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        filterTimeIndices.add(which);
                    } else {
                        filterTimeIndices.remove(which);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private String formatSelectedDays() {
        if (selectedDayIndices.isEmpty()) {
            return "";
        }
        
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        List<String> selectedDays = new ArrayList<>();
        
        for (Integer index : selectedDayIndices) {
            if (index < daysArray.length) {
                selectedDays.add(daysArray[index].substring(0, 3)); // Get first 3 letters (Mon, Tue, etc.)
            }
        }
        
        return TextUtils.join(", ", selectedDays);
    }
    
    private String formatSelectedTimes() {
        if (selectedTimeIndices.isEmpty()) {
            return "";
        }
        
        String[] timesArray = getResources().getStringArray(R.array.time_periods);
        List<String> selectedTimes = new ArrayList<>();
        
        for (Integer index : selectedTimeIndices) {
            if (index < timesArray.length) {
                selectedTimes.add(timesArray[index]);
            }
        }
        
        return TextUtils.join("; ", selectedTimes);
    }
    
    private void updateSelectedDaysDisplay() {
        String formattedDays = formatSelectedDays();
        if (TextUtils.isEmpty(formattedDays)) {
            tvSelectedDays.setVisibility(TextView.GONE);
            btnSelectDays.setText(R.string.find_belayer_days_hint);
        } else {
            tvSelectedDays.setText(getString(R.string.find_belayer_selected_days, formattedDays));
            tvSelectedDays.setVisibility(TextView.VISIBLE);
            btnSelectDays.setText(R.string.find_belayer_days_hint);
        }
    }
    
    private void updateSelectedTimesDisplay() {
        String formattedTimes = formatSelectedTimes();
        if (TextUtils.isEmpty(formattedTimes)) {
            tvSelectedTimes.setVisibility(TextView.GONE);
            btnSelectTimes.setText(R.string.find_belayer_times_hint);
        } else {
            tvSelectedTimes.setText(getString(R.string.find_belayer_selected_times, formattedTimes));
            tvSelectedTimes.setVisibility(TextView.VISIBLE);
            btnSelectTimes.setText(R.string.find_belayer_times_hint);
        }
    }
    
    private List<BelayerPost> applyFilters(List<BelayerPost> postsToFilter) {
        // If no filters are applied, return all posts
        if (filterDayIndices.isEmpty() && filterTimeIndices.isEmpty()) {
            return postsToFilter;
        }
        
        List<BelayerPost> filteredPosts = new ArrayList<>();
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        
        for (BelayerPost post : postsToFilter) {
            boolean matchesDay = filterDayIndices.isEmpty();
            boolean matchesTime = filterTimeIndices.isEmpty();
            
            // Check days
            if (!filterDayIndices.isEmpty() && !TextUtils.isEmpty(post.getClimbDays())) {
                String postDays = post.getClimbDays().toLowerCase();
                for (Integer dayIndex : filterDayIndices) {
                    if (dayIndex < daysArray.length) {
                        String dayAbbr = daysArray[dayIndex].substring(0, 3).toLowerCase();
                        if (postDays.contains(dayAbbr)) {
                            matchesDay = true;
                            break;
                        }
                    }
                }
            }
            
            // Check times
            if (!filterTimeIndices.isEmpty() && !TextUtils.isEmpty(post.getClimbTimes())) {
                String postTimes = post.getClimbTimes().toLowerCase();
                for (Integer timeIndex : filterTimeIndices) {
                    // Simple check - if post times contain any of the filter time periods' hours
                    String[] timePeriods = getResources().getStringArray(R.array.time_periods);
                    if (timeIndex < timePeriods.length) {
                        String timePeriod = timePeriods[timeIndex];
                        // Extract hours from time period (e.g., "6:00" from "Early morning (6:00 - 9:00)")
                        if (postTimes.contains("18") || postTimes.contains("19") || postTimes.contains("20") || postTimes.contains("21")) {
                            if (timePeriod.contains("18") || timePeriod.contains("19") || timePeriod.contains("20") || timePeriod.contains("21")) {
                                matchesTime = true;
                                break;
                            }
                        } else if (postTimes.contains("6") || postTimes.contains("7") || postTimes.contains("8") || postTimes.contains("9")) {
                            if (timePeriod.contains("6") || timePeriod.contains("7") || timePeriod.contains("8") || timePeriod.contains("9")) {
                                matchesTime = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (matchesDay && matchesTime) {
                filteredPosts.add(post);
            }
        }
        
        return filteredPosts;
    }
    
    private void applyFiltersAndRefresh() {
        List<BelayerPost> filteredPosts = applyFilters(allPosts);
        belayerPostAdapter.submitList(filteredPosts);
        updateSummary(filteredPosts);
        
        boolean hasPosts = !filteredPosts.isEmpty();
        rvBelayerPosts.setVisibility(hasPosts ? RecyclerView.VISIBLE : RecyclerView.GONE);
        tvBelayerPostsEmpty.setVisibility(hasPosts ? TextView.GONE : TextView.VISIBLE);
        if (!hasPosts) {
            tvBelayerPostsEmpty.setText(R.string.find_belayer_empty_history);
        }
        
        updateFilterStatus();
    }
    
    private void updateFilterStatus() {
        if (filterDayIndices.isEmpty() && filterTimeIndices.isEmpty()) {
            tvFilterStatus.setText(R.string.find_belayer_no_filter);
        } else {
            String daysText = formatFilteredDays();
            String timesText = formatFilteredTimes();
            
            if (!TextUtils.isEmpty(daysText) && !TextUtils.isEmpty(timesText)) {
                tvFilterStatus.setText(getString(R.string.find_belayer_selected_days, daysText) + "\n" + getString(R.string.find_belayer_selected_times, timesText));
            } else if (!TextUtils.isEmpty(daysText)) {
                tvFilterStatus.setText(getString(R.string.find_belayer_selected_days, daysText));
            } else if (!TextUtils.isEmpty(timesText)) {
                tvFilterStatus.setText(getString(R.string.find_belayer_selected_times, timesText));
            }
        }
    }
    
    private String formatFilteredDays() {
        if (filterDayIndices.isEmpty()) {
            return "";
        }
        
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        List<String> selectedDays = new ArrayList<>();
        
        for (Integer index : filterDayIndices) {
            if (index < daysArray.length) {
                selectedDays.add(daysArray[index].substring(0, 3));
            }
        }
        
        return TextUtils.join(", ", selectedDays);
    }
    
    private String formatFilteredTimes() {
        if (filterTimeIndices.isEmpty()) {
            return "";
        }
        
        String[] timesArray = getResources().getStringArray(R.array.time_periods);
        List<String> selectedTimes = new ArrayList<>();
        
        for (Integer index : filterTimeIndices) {
            if (index < timesArray.length) {
                // Extract the time range from the period (e.g., "6:00 - 9:00" from "Early morning (6:00 - 9:00)")
                String period = timesArray[index];
                int startParen = period.indexOf("(");
                int endParen = period.indexOf(")");
                if (startParen != -1 && endParen != -1) {
                    selectedTimes.add(period.substring(startParen + 1, endParen));
                } else {
                    selectedTimes.add(period);
                }
            }
        }
        
        return TextUtils.join("; ", selectedTimes);
    }

    private boolean isServerAccessBlocked() {
        return ServerFeatureGate.isServerFeatureBlocked(this);
    }
}
