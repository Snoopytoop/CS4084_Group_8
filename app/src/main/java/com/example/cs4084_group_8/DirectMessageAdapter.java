package com.example.cs4084_group_8;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DirectMessageAdapter extends ListAdapter<DirectMessage, DirectMessageAdapter.DirectMessageViewHolder> {
    private static final DiffUtil.ItemCallback<DirectMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<DirectMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull DirectMessage oldItem, @NonNull DirectMessage newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DirectMessage oldItem, @NonNull DirectMessage newItem) {
            return Objects.equals(oldItem.getText(), newItem.getText())
                    && Objects.equals(oldItem.getSenderUid(), newItem.getSenderUid())
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt())
                    && oldItem.isPendingWrite() == newItem.isPendingWrite();
        }
    };

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final LayoutInflater layoutInflater;
    private final String currentUserUid;

    public DirectMessageAdapter(LayoutInflater layoutInflater, String currentUserUid) {
        super(DIFF_CALLBACK);
        this.layoutInflater = layoutInflater;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public DirectMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_direct_message, parent, false);
        return new DirectMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectMessageViewHolder holder, int position) {
        DirectMessage message = getItem(position);
        boolean outgoing = TextUtils.equals(currentUserUid, message.getSenderUid());

        LinearLayout.LayoutParams containerParams =
                (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        containerParams.gravity = outgoing ? Gravity.END : Gravity.START;
        holder.messageContainer.setLayoutParams(containerParams);

        holder.tvSender.setVisibility(outgoing ? View.GONE : View.VISIBLE);
        holder.tvSender.setText(message.getSenderName());
        holder.tvBody.setText(message.getText());
        holder.tvTime.setText(formatTimestamp(message.getCreatedAt()));

        if (outgoing) {
            holder.tvDeliveryState.setVisibility(View.VISIBLE);
            if (message.isPendingWrite()) {
                holder.tvDeliveryState.setText(holder.itemView.getContext().getString(R.string.chat_message_status_pending));
                holder.tvDeliveryState.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.route_log_text_secondary)
                );
            } else {
                holder.tvDeliveryState.setText(holder.itemView.getContext().getString(R.string.chat_message_status_sent_ticks));
                holder.tvDeliveryState.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_tick_blue)
                );
            }
        } else {
            holder.tvDeliveryState.setVisibility(View.GONE);
        }

        int cardColor = outgoing
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.route_log_accent)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.route_log_card_bg);
        int bodyColor = outgoing
                ? Color.BLACK
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.route_log_text_primary);
        int timeColor = outgoing
                ? Color.argb(180, 0, 0, 0)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.route_log_text_secondary);

        holder.messageCard.setCardBackgroundColor(cardColor);
        holder.tvBody.setTextColor(bodyColor);
        holder.tvTime.setTextColor(timeColor);
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return TIMESTAMP_FORMAT.format(timestamp.toDate());
    }

    static class DirectMessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageContainer;
        private final TextView tvSender;
        private final MaterialCardView messageCard;
        private final TextView tvBody;
        private final TextView tvTime;
        private final TextView tvDeliveryState;

        DirectMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvSender = itemView.findViewById(R.id.tvMessageSender);
            messageCard = itemView.findViewById(R.id.messageCard);
            tvBody = itemView.findViewById(R.id.tvMessageBody);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
            tvDeliveryState = itemView.findViewById(R.id.tvMessageDeliveryState);
        }
    }
}
