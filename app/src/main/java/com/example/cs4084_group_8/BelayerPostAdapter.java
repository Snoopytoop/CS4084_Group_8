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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BelayerPostAdapter extends ListAdapter<BelayerPost, BelayerPostAdapter.BelayerPostViewHolder> {
    private static final DiffUtil.ItemCallback<BelayerPost> DIFF_CALLBACK = new DiffUtil.ItemCallback<BelayerPost>() {
        @Override
        public boolean areItemsTheSame(@NonNull BelayerPost oldItem, @NonNull BelayerPost newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BelayerPost oldItem, @NonNull BelayerPost newItem) {
            return Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName())
                    && Objects.equals(oldItem.getWallName(), newItem.getWallName())
                    && Objects.equals(oldItem.getClimbDays(), newItem.getClimbDays())
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }
    };

    public interface ActionListener {
        void onMessage(BelayerPost post);

        void onDeletePost(BelayerPost post);

        void onViewProfile(BelayerPost post);
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
    private final LayoutInflater layoutInflater;
    private final ActionListener actionListener;
    private final String currentUserUid;

    public BelayerPostAdapter(LayoutInflater layoutInflater, ActionListener actionListener, String currentUserUid) {
        super(DIFF_CALLBACK);
        this.layoutInflater = layoutInflater;
        this.actionListener = actionListener;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public BelayerPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_belayer_post, parent, false);
        return new BelayerPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BelayerPostViewHolder holder, int position) {
        BelayerPost post = getItem(position);
        holder.tvDisplayName.setText(post.getAuthorName());
        holder.tvPostedTime.setText(formatTimestamp(post.getCreatedAt()));
        holder.tvWallName.setText(
                holder.itemView.getContext().getString(R.string.find_belayer_wall_format, post.getWallName())
        );
        holder.tvDaysChip.setText(
                holder.itemView.getContext().getString(R.string.find_belayer_days_chip_format, post.getClimbDays())
        );
        holder.tvTimeChip.setText(
                holder.itemView.getContext().getString(R.string.find_belayer_time_chip_format, post.getClimbTimes())
        );
        holder.tvBelayCapability.setText(
                holder.itemView.getContext().getString(R.string.find_belayer_belay_format, post.getBelayCapability())
        );
        holder.tvClimbCapability.setText(
                holder.itemView.getContext().getString(R.string.find_belayer_climb_format, post.getClimbCapability())
        );

        if (TextUtils.isEmpty(post.getNotes())) {
            holder.tvNotes.setVisibility(View.GONE);
        } else {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(post.getNotes());
        }

        holder.tvDisplayName.setOnClickListener(view -> actionListener.onViewProfile(post));
        holder.btnMessageBelayer.setOnClickListener(view -> actionListener.onMessage(post));

        boolean isOwner = !TextUtils.isEmpty(currentUserUid) && currentUserUid.equals(post.getAuthorUid());
        holder.btnMessageBelayer.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        holder.btnDeleteBelayerPost.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnDeleteBelayerPost.setOnClickListener(view -> actionListener.onDeletePost(post));
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return TIMESTAMP_FORMAT.format(timestamp.toDate());
    }

    static class BelayerPostViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDisplayName;
        private final TextView tvPostedTime;
        private final TextView tvWallName;
        private final TextView tvDaysChip;
        private final TextView tvTimeChip;
        private final TextView tvBelayCapability;
        private final TextView tvClimbCapability;
        private final TextView tvNotes;
        private final MaterialButton btnMessageBelayer;
        private final MaterialButton btnDeleteBelayerPost;

        BelayerPostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDisplayName = itemView.findViewById(R.id.tvBelayerName);
            tvPostedTime = itemView.findViewById(R.id.tvBelayerPostedTime);
            tvWallName = itemView.findViewById(R.id.tvBelayerWall);
            tvDaysChip = itemView.findViewById(R.id.tvBelayerDaysChip);
            tvTimeChip = itemView.findViewById(R.id.tvBelayerTimeChip);
            tvBelayCapability = itemView.findViewById(R.id.tvBelayerBelayCapability);
            tvClimbCapability = itemView.findViewById(R.id.tvBelayerClimbCapability);
            tvNotes = itemView.findViewById(R.id.tvBelayerNotes);
            btnMessageBelayer = itemView.findViewById(R.id.btnMessageBelayer);
            btnDeleteBelayerPost = itemView.findViewById(R.id.btnDeleteBelayerPost);
        }
    }
}
