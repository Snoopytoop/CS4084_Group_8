package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
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

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ConversationSummaryAdapter conversationSummaryAdapter;
    private ListenerRegistration conversationsListener;

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

                    List<ConversationSummary> conversations = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            ConversationSummary conversation = documentSnapshot.toObject(ConversationSummary.class);
                            if (conversation != null) {
                                conversation.setId(documentSnapshot.getId());
                                conversations.add(conversation);
                            }
                        });
                    }

                    conversations.sort(Comparator.comparing(
                            ConversationSummary::getLastMessageAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());

                    conversationSummaryAdapter.submitConversations(conversations);

                    boolean hasConversations = !conversations.isEmpty();
                    rvInboxConversations.setVisibility(hasConversations ? RecyclerView.VISIBLE : RecyclerView.GONE);
                    tvInboxEmpty.setVisibility(hasConversations ? TextView.GONE : TextView.VISIBLE);
                    if (!hasConversations) {
                        tvInboxEmpty.setText(R.string.inbox_empty_state);
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
}
