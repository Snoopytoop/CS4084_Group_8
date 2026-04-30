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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RouteLogAdapter extends ListAdapter<RouteLogEntry, RouteLogAdapter.RouteLogViewHolder> {
    private static final DiffUtil.ItemCallback<RouteLogEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<RouteLogEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouteLogEntry oldItem, @NonNull RouteLogEntry newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouteLogEntry oldItem, @NonNull RouteLogEntry newItem) {
            return Objects.equals(oldItem.getRouteName(), newItem.getRouteName())
                    && Objects.equals(oldItem.getGrade(), newItem.getGrade())
                    && oldItem.getAttempts() == newItem.getAttempts()
                    && Objects.equals(oldItem.getSendStatus(), newItem.getSendStatus())
                    && oldItem.getSortTimestampMillis() == newItem.getSortTimestampMillis();
        }
    };

    public interface OnDeleteClickListener {
        void onDeleteClick(RouteLogEntry entry);
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault());
    private final LayoutInflater layoutInflater;
    private final OnDeleteClickListener onDeleteClickListener;

    public RouteLogAdapter(LayoutInflater layoutInflater, OnDeleteClickListener onDeleteClickListener) {
        super(DIFF_CALLBACK);
        this.layoutInflater = layoutInflater;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public RouteLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_route_log_entry, parent, false);
        return new RouteLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteLogViewHolder holder, int position) {
        RouteLogEntry entry = getItem(position);
        holder.tvRouteName.setText(entry.getRouteName());
        holder.tvRouteGrade.setText(
                holder.itemView.getContext().getString(R.string.route_log_grade_chip_format, entry.getGrade())
        );
        holder.tvRouteAttempts.setText(
                holder.itemView.getContext().getString(R.string.route_log_attempts_chip_format, entry.getAttempts())
        );
        holder.tvRouteStatus.setText(entry.getSendStatus());
        holder.tvRouteLoggedAt.setText(formatTimestamp(entry));

        if (TextUtils.isEmpty(entry.getNotes())) {
            holder.tvRouteNotes.setVisibility(View.GONE);
        } else {
            holder.tvRouteNotes.setVisibility(View.VISIBLE);
            holder.tvRouteNotes.setText(entry.getNotes());
        }

        holder.btnDeleteRouteEntry.setOnClickListener(view -> onDeleteClickListener.onDeleteClick(entry));
    }

    private String formatTimestamp(RouteLogEntry entry) {
        long timestampMillis = entry.getSortTimestampMillis();
        if (timestampMillis <= 0L) {
            return "";
        }
        return TIMESTAMP_FORMAT.format(new Date(timestampMillis));
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
