package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InboxActivity extends AppCompatActivity {
    private MaterialToolbar toolbarInbox;
    private TextView tvInboxEmpty;
    private RecyclerView rvInboxConversations;
    private RecyclerView rvUserSearch;
    private TextInputEditText etInboxSearch;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ConversationSummaryAdapter conversationSummaryAdapter;
    private UserSearchAdapter userSearchAdapter;
    private ListenerRegistration conversationsListener;
    private List<ConversationSummary> allConversations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToAuth();
            return;
        }

        bindViews();
        configureToolbar();
        configureList();
        configureSearch();
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToAuth();
            return;
        }
        listenForConversations();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }

    private void bindViews() {
        toolbarInbox = findViewById(R.id.toolbarInbox);
        tvInboxEmpty = findViewById(R.id.tvInboxEmpty);
        rvInboxConversations = findViewById(R.id.rvInboxConversations);
        rvUserSearch = findViewById(R.id.rvUserSearch);
        etInboxSearch = findViewById(R.id.etInboxSearch);
    }

    private void configureToolbar() {
        toolbarInbox.setNavigationIcon(R.drawable.ic_route_back);
        toolbarInbox.setNavigationOnClickListener(view -> finish());
    }

    private void configureList() {
        rvInboxConversations.setLayoutManager(new LinearLayoutManager(this));
        conversationSummaryAdapter = new ConversationSummaryAdapter(
                getLayoutInflater(),
                currentUser.getUid(),
                (conversation, otherUserId, otherUserName) -> openConversation(otherUserId, otherUserName)
        );
        rvInboxConversations.setAdapter(conversationSummaryAdapter);

        rvUserSearch.setLayoutManager(new LinearLayoutManager(this));
        userSearchAdapter = new UserSearchAdapter();
        rvUserSearch.setAdapter(userSearchAdapter);
    }

    private void configureSearch() {
        etInboxSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    rvUserSearch.setVisibility(View.GONE);
                    boolean hasConversations = !allConversations.isEmpty();
                    rvInboxConversations.setVisibility(hasConversations ? View.VISIBLE : View.GONE);
                    tvInboxEmpty.setVisibility(hasConversations ? View.GONE : View.VISIBLE);
                    if (!hasConversations) tvInboxEmpty.setText(R.string.inbox_empty_state);
                } else {
                    rvInboxConversations.setVisibility(View.GONE);
                    tvInboxEmpty.setVisibility(View.GONE);
                    rvUserSearch.setVisibility(View.VISIBLE);
                    searchUsers(query);
                }
            }
        });
    }

    private void searchUsers(String query) {
        String queryLower = query.toLowerCase();
        firestore.collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "")
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    List<UserSearchResult> results = new ArrayList<>();
                    snap.getDocuments().forEach(doc -> {
                        String uid = doc.getId();
                        String username = doc.getString("username");
                        if (username != null && !uid.equals(currentUser.getUid())) {
                            results.add(new UserSearchResult(uid, username));
                        }
                    });
                    userSearchAdapter.setResults(results);
                    if (results.isEmpty()) {
                        tvInboxEmpty.setText("No users found");
                        tvInboxEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvInboxEmpty.setVisibility(View.GONE);
                    }
                });
    }

    private void listenForConversations() {
        if (conversationsListener != null) {
            conversationsListener.remove();
        }

        tvInboxEmpty.setVisibility(TextView.VISIBLE);
        rvInboxConversations.setVisibility(RecyclerView.GONE);

        conversationsListener = firestore.collection(FirestoreCollections.CONVERSATIONS)
                .whereArrayContains("participants", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvInboxEmpty.setVisibility(TextView.VISIBLE);
                        rvInboxConversations.setVisibility(RecyclerView.GONE);
                        tvInboxEmpty.setText(getString(R.string.inbox_load_failed, error.getMessage()));
                        return;
                    }

                    allConversations.clear();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            ConversationSummary conversation = documentSnapshot.toObject(ConversationSummary.class);
                            if (conversation != null) {
                                conversation.setId(documentSnapshot.getId());
                                allConversations.add(conversation);
                            }
                        });
                    }

                    allConversations.sort(Comparator.comparing(
                            ConversationSummary::getLastMessageAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());

                    // Only update conversation list if search is not active
                    String searchText = etInboxSearch != null && etInboxSearch.getText() != null
                            ? etInboxSearch.getText().toString().trim() : "";
                    if (searchText.isEmpty()) {
                        conversationSummaryAdapter.submitConversations(allConversations);
                        boolean hasConversations = !allConversations.isEmpty();
                        rvInboxConversations.setVisibility(hasConversations ? RecyclerView.VISIBLE : RecyclerView.GONE);
                        tvInboxEmpty.setVisibility(hasConversations ? TextView.GONE : TextView.VISIBLE);
                        if (!hasConversations) tvInboxEmpty.setText(R.string.inbox_empty_state);
                    } else {
                        conversationSummaryAdapter.submitConversations(allConversations);
                    }
                });
    }

    private void openConversation(String otherUserId, String otherUserName) {
        if (TextUtils.isEmpty(otherUserId)) {
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, otherUserName);
        startActivity(intent);
    }

    private void navigateToAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private static class UserSearchResult {
        final String uid;
        final String username;

        UserSearchResult(String uid, String username) {
            this.uid = uid;
            this.username = username;
        }
    }

    private class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {
        private List<UserSearchResult> results = new ArrayList<>();

        void setResults(List<UserSearchResult> newResults) {
            results = newResults;
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            ((TextView) view.findViewById(android.R.id.text1))
                    .setTextColor(getResources().getColor(R.color.route_log_text_primary, getTheme()));
            view.setPadding(48, 32, 48, 32);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            UserSearchResult result = results.get(position);
            holder.tvName.setText(result.username);
            holder.itemView.setOnClickListener(v -> openConversation(result.uid, result.username));
        }

        @Override
        public int getItemCount() { return results.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvName;
            VH(View v) {
                super(v);
                tvName = v.findViewById(android.R.id.text1);
            }
        }
    }
}
