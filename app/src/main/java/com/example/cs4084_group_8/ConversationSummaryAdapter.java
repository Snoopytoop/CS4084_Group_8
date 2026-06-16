package com.example.cs4084_group_8;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ConversationSummaryAdapter extends ListAdapter<ConversationSummary, ConversationSummaryAdapter.ConversationViewHolder> {
    private static final DiffUtil.ItemCallback<ConversationSummary> DIFF_CALLBACK = new DiffUtil.ItemCallback<ConversationSummary>() {
        @Override
        public boolean areItemsTheSame(@NonNull ConversationSummary oldItem, @NonNull ConversationSummary newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ConversationSummary oldItem, @NonNull ConversationSummary newItem) {
            return Objects.equals(oldItem.getLastMessageText(), newItem.getLastMessageText())
                    && Objects.equals(oldItem.getLastMessageAt(), newItem.getLastMessageAt())
                    && Objects.equals(oldItem.getMemberNames(), newItem.getMemberNames());
        }
    };

    public interface ActionListener {
        void onOpenConversation(ConversationSummary conversation, String otherUserId, String otherUserName);
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
    private final LayoutInflater layoutInflater;
    private final String currentUserUid;
    private final ActionListener actionListener;

    public ConversationSummaryAdapter(
            LayoutInflater layoutInflater,
            String currentUserUid,
            ActionListener actionListener
    ) {
        super(DIFF_CALLBACK);
        this.layoutInflater = layoutInflater;
        this.currentUserUid = currentUserUid;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationSummary conversation = getItem(position);
        String otherUserId = getOtherUserId(conversation);
        String otherUserName = getOtherUserName(conversation, otherUserId);

        holder.tvConversationName.setText(otherUserName);
        holder.tvConversationPreview.setText(firstNonEmpty(
                conversation.getLastMessageText(),
                holder.itemView.getContext().getString(R.string.inbox_empty_preview)
        ));
        holder.tvConversationTime.setText(formatTimestamp(conversation.getLastMessageAt()));
        holder.itemView.setOnClickListener(view ->
                actionListener.onOpenConversation(conversation, otherUserId, otherUserName)
        );
    }

    private String getOtherUserId(ConversationSummary conversation) {
        List<String> participants = conversation.getParticipants();
        if (participants == null) {
            return "";
        }
        for (String participant : participants) {
            if (!TextUtils.equals(currentUserUid, participant)) {
                return participant;
            }
        }
        return "";
    }

    private String getOtherUserName(ConversationSummary conversation, String otherUserId) {
        if (conversation.getMemberNames() == null || TextUtils.isEmpty(otherUserId)) {
            return firstNonEmpty(null, layoutInflater.getContext().getString(R.string.chat_other_user_fallback));
        }
        return firstNonEmpty(
                conversation.getMemberNames().get(otherUserId),
                layoutInflater.getContext().getString(R.string.chat_other_user_fallback)
        );
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return TIMESTAMP_FORMAT.format(timestamp.toDate());
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvConversationName;
        private final TextView tvConversationPreview;
        private final TextView tvConversationTime;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationName = itemView.findViewById(R.id.tvConversationName);
            tvConversationPreview = itemView.findViewById(R.id.tvConversationPreview);
            tvConversationTime = itemView.findViewById(R.id.tvConversationTime);
        }
    }
}
