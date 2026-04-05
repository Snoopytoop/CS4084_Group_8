package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_OTHER_USER_ID = "extra_other_user_id";
    public static final String EXTRA_OTHER_USER_NAME = "extra_other_user_name";

    private MaterialToolbar toolbarChat;
    private TextView tvChatEmpty;
    private RecyclerView rvChatMessages;
    private TextInputLayout tilChatMessage;
    private TextInputEditText etChatMessage;
    private MaterialButton btnSendChatMessage;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private DirectMessageAdapter directMessageAdapter;
    private ListenerRegistration messagesListener;
    private LinearLayoutManager linearLayoutManager;

    private String otherUserId;
    private String otherUserName;
    private String currentUserName;
    private String otherUserProfileImageUrl;
    private String currentUserProfileImageUrl;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);

        if (currentUser == null) {
            navigateToAuth();
            return;
        }
        if (TextUtils.isEmpty(otherUserId) || TextUtils.equals(currentUser.getUid(), otherUserId)) {
            Toast.makeText(this, R.string.chat_invalid_target, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        conversationId = ConversationThreadIds.buildConversationId(currentUser.getUid(), otherUserId);

        bindViews();
        configureToolbar();
        configureMessagesList();
        loadParticipantNames();

        btnSendChatMessage.setOnClickListener(view -> sendMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToAuth();
            return;
        }
        listenForMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void bindViews() {
        toolbarChat = findViewById(R.id.toolbarChat);
        tvChatEmpty = findViewById(R.id.tvChatEmpty);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        tilChatMessage = findViewById(R.id.tilChatMessage);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendChatMessage = findViewById(R.id.btnSendChatMessage);
    }

    private void configureToolbar() {
        toolbarChat.setNavigationIcon(R.drawable.ic_route_back);
        toolbarChat.setTitle(firstNonEmpty(otherUserName, getString(R.string.chat_title_default)));
        toolbarChat.setSubtitle(R.string.chat_subtitle);
        toolbarChat.setNavigationOnClickListener(view -> finish());
    }

    private void configureMessagesList() {
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(false);
        rvChatMessages.setLayoutManager(linearLayoutManager);
        directMessageAdapter = new DirectMessageAdapter(getLayoutInflater(), currentUser.getUid());
        rvChatMessages.setAdapter(directMessageAdapter);
    }

    private void loadParticipantNames() {
        DocumentReference currentUserRef = firestore.collection(FirestoreCollections.USERS)
                .document(currentUser.getUid());
        DocumentReference otherUserRef = firestore.collection(FirestoreCollections.USERS)
                .document(otherUserId);

        currentUserRef.get()
                .addOnSuccessListener(snapshot -> {
                    currentUserName = firstNonEmpty(
                            snapshot.getString("username"),
                            currentUser.getDisplayName(),
                            currentUser.getEmail(),
                            getString(R.string.chat_current_user_fallback)
                    );
                    currentUserProfileImageUrl = snapshot.getString("profileImageUrl");
                });

        otherUserRef.get()
                .addOnSuccessListener(snapshot -> {
                    otherUserName = firstNonEmpty(
                            snapshot.getString("username"),
                            otherUserName,
                            snapshot.getString("email"),
                            getString(R.string.chat_other_user_fallback)
                    );
                    otherUserProfileImageUrl = snapshot.getString("profileImageUrl");
                    toolbarChat.setTitle(otherUserName);
                })
                .addOnFailureListener(e ->
                        toolbarChat.setTitle(firstNonEmpty(otherUserName, getString(R.string.chat_other_user_fallback)))
                );
    }

    private void listenForMessages() {
        if (messagesListener != null) {
            messagesListener.remove();
        }

        tvChatEmpty.setVisibility(TextView.VISIBLE);
        rvChatMessages.setVisibility(RecyclerView.GONE);

        messagesListener = firestore.collection(FirestoreCollections.CONVERSATIONS)
                .document(conversationId)
                .collection(FirestoreCollections.MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvChatEmpty.setVisibility(TextView.VISIBLE);
                        rvChatMessages.setVisibility(RecyclerView.GONE);
                        tvChatEmpty.setText(getString(R.string.chat_load_failed, error.getMessage()));
                        return;
                    }

                    List<DirectMessage> messages = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            DirectMessage message = documentSnapshot.toObject(DirectMessage.class);
                            if (message != null) {
                                message.setId(documentSnapshot.getId());
                                messages.add(message);
                            }
                        });
                    }

                    directMessageAdapter.submitMessages(messages);

                    boolean hasMessages = !messages.isEmpty();
                    rvChatMessages.setVisibility(hasMessages ? RecyclerView.VISIBLE : RecyclerView.GONE);
                    tvChatEmpty.setVisibility(hasMessages ? TextView.GONE : TextView.VISIBLE);
                    if (!hasMessages) {
                        tvChatEmpty.setText(R.string.chat_empty_state);
                    } else {
                        rvChatMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        tilChatMessage.setError(null);

        String messageText = valueOf(etChatMessage);
        if (TextUtils.isEmpty(messageText)) {
            tilChatMessage.setError(getString(R.string.chat_message_required));
            return;
        }

        btnSendChatMessage.setEnabled(false);

        String senderName = firstNonEmpty(
                currentUserName,
                currentUser.getDisplayName(),
                currentUser.getEmail(),
                getString(R.string.chat_current_user_fallback)
        );
        String receiverName = firstNonEmpty(otherUserName, getString(R.string.chat_other_user_fallback));

        DocumentReference conversationRef = firestore.collection(FirestoreCollections.CONVERSATIONS)
                .document(conversationId);
        DocumentReference messageRef = conversationRef.collection(FirestoreCollections.MESSAGES)
                .document();

        Map<String, String> memberNames = new HashMap<>();
        memberNames.put(currentUser.getUid(), senderName);
        memberNames.put(otherUserId, receiverName);

        Map<String, String> memberProfileImageUrls = new HashMap<>();
        memberProfileImageUrls.put(currentUser.getUid(), firstNonEmpty(currentUserProfileImageUrl));
        memberProfileImageUrls.put(otherUserId, firstNonEmpty(otherUserProfileImageUrl));

        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("participants", ConversationThreadIds.buildParticipants(currentUser.getUid(), otherUserId));
        conversationData.put("memberNames", memberNames);
        conversationData.put("memberProfileImageUrls", memberProfileImageUrls);
        conversationData.put("lastMessageText", messageText);
        conversationData.put("lastMessageSenderUid", currentUser.getUid());
        conversationData.put("lastMessageAt", FieldValue.serverTimestamp());

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderUid", currentUser.getUid());
        messageData.put("senderName", senderName);
        messageData.put("text", messageText);
        messageData.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(conversationRef, conversationData, SetOptions.merge());
        batch.set(messageRef, messageData);
        batch.commit()
                .addOnSuccessListener(unused -> {
                    btnSendChatMessage.setEnabled(true);
                    etChatMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    btnSendChatMessage.setEnabled(true);
                    Toast.makeText(this, getString(R.string.chat_send_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private String valueOf(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }
}
