package com.example.cs4084_group_8;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BelayerPostAdapter extends RecyclerView.Adapter<BelayerPostAdapter.BelayerPostViewHolder> {
    public interface OnMessageClickListener {
        void onMessageClick(BelayerPost post);
    }

    private final List<BelayerPost> posts = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final OnMessageClickListener onMessageClickListener;
    private final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());

    public BelayerPostAdapter(LayoutInflater layoutInflater, OnMessageClickListener onMessageClickListener) {
        this.layoutInflater = layoutInflater;
        this.onMessageClickListener = onMessageClickListener;
    }

    public void submitPosts(List<BelayerPost> belayerPosts) {
        posts.clear();
        posts.addAll(belayerPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BelayerPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_belayer_post, parent, false);
        return new BelayerPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BelayerPostViewHolder holder, int position) {
        BelayerPost post = posts.get(position);
        holder.tvDisplayName.setText(post.getDisplayName());
        holder.tvPostedTime.setText(timestampFormat.format(new Date(post.getCreatedAtMillis())));
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

        holder.btnMessageBelayer.setOnClickListener(view -> onMessageClickListener.onMessageClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
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
        }
    }
}
