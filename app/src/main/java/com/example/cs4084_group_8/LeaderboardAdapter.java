package com.example.cs4084_group_8;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    public interface OnEntryLongClickListener {
        void onEntryLongClick(LeaderboardEntry entry);
    }

    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private OnEntryLongClickListener longClickListener;
    private String currentUserId;

    public void setEntries(List<LeaderboardEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    public void setOnEntryLongClickListener(OnEntryLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvPlayerName.setText(entry.getUsername() != null && !entry.getUsername().isEmpty() ? entry.getUsername() : "Unknown");

        long seconds = entry.getSeconds();
        long milliseconds = entry.getMilliseconds();
        holder.tvTime.setText(String.format(Locale.getDefault(), "%d.%03ds", seconds, milliseconds));

        String dateText = "-";
        if (entry.getCreatedAt() != null) {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText = format.format(entry.getCreatedAt().toDate());
        }
        holder.tvDate.setText(dateText);

        if (currentUserId != null && currentUserId.equals(entry.getUid())) {
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onEntryLongClick(entry);
                }
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvPlayerName;
        TextView tvTime;
        TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
