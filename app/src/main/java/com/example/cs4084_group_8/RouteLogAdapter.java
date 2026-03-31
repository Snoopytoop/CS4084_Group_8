package com.example.cs4084_group_8;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteLogAdapter extends RecyclerView.Adapter<RouteLogAdapter.RouteLogViewHolder> {
    public interface OnDeleteClickListener {
        void onDeleteClick(RouteLogEntry entry);
    }

    private final List<RouteLogEntry> entries = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final OnDeleteClickListener onDeleteClickListener;
    private final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault());

    public RouteLogAdapter(LayoutInflater layoutInflater, OnDeleteClickListener onDeleteClickListener) {
        this.layoutInflater = layoutInflater;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void submitEntries(List<RouteLogEntry> routeEntries) {
        entries.clear();
        entries.addAll(routeEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_route_log_entry, parent, false);
        return new RouteLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteLogViewHolder holder, int position) {
        RouteLogEntry entry = entries.get(position);
        holder.tvRouteName.setText(entry.getRouteName());
        holder.tvRouteGrade.setText(
                holder.itemView.getContext().getString(R.string.route_log_grade_chip_format, entry.getGrade())
        );
        holder.tvRouteAttempts.setText(
                holder.itemView.getContext().getString(R.string.route_log_attempts_chip_format, entry.getAttempts())
        );
        holder.tvRouteStatus.setText(entry.getSendStatus());
        holder.tvRouteLoggedAt.setText(formatTimestamp(entry.getLoggedAt()));

        if (TextUtils.isEmpty(entry.getNotes())) {
            holder.tvRouteNotes.setVisibility(View.GONE);
        } else {
            holder.tvRouteNotes.setVisibility(View.VISIBLE);
            holder.tvRouteNotes.setText(entry.getNotes());
        }

        holder.btnDeleteRouteEntry.setOnClickListener(view -> onDeleteClickListener.onDeleteClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestampFormat.format(timestamp.toDate());
    }

    static class RouteLogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRouteName;
        private final TextView tvRouteGrade;
        private final TextView tvRouteAttempts;
        private final TextView tvRouteStatus;
        private final TextView tvRouteLoggedAt;
        private final TextView tvRouteNotes;
        private final MaterialButton btnDeleteRouteEntry;

        RouteLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvRouteGrade = itemView.findViewById(R.id.tvRouteGrade);
            tvRouteAttempts = itemView.findViewById(R.id.tvRouteAttempts);
            tvRouteStatus = itemView.findViewById(R.id.tvRouteStatus);
            tvRouteLoggedAt = itemView.findViewById(R.id.tvRouteLoggedAt);
            tvRouteNotes = itemView.findViewById(R.id.tvRouteNotes);
            btnDeleteRouteEntry = itemView.findViewById(R.id.btnDeleteRouteEntry);
        }
    }
}
