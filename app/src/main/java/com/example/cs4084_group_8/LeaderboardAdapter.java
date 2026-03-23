package com.example.cs4084_group_8;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private final List<LeaderboardEntry> entries = new ArrayList<>();

    public void setEntries(List<LeaderboardEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
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
        holder.tvTime.setText(String.format("%d.%03ds", seconds, milliseconds));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvPlayerName;
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
