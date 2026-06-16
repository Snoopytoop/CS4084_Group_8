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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BlogPostAdapter extends ListAdapter<BlogPost, BlogPostAdapter.BlogPostViewHolder> {
    private static final DiffUtil.ItemCallback<BlogPost> DIFF_CALLBACK = new DiffUtil.ItemCallback<BlogPost>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlogPost oldItem, @NonNull BlogPost newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlogPost oldItem, @NonNull BlogPost newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getBody(), newItem.getBody())
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }
    };

    public interface ActionListener {
        void onDeletePost(BlogPost post);

        void onViewProfile(BlogPost post);
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault());
    private final LayoutInflater layoutInflater;
    private final ActionListener actionListener;
    private final String currentUserUid;

    public BlogPostAdapter(LayoutInflater layoutInflater, ActionListener actionListener, String currentUserUid) {
        super(DIFF_CALLBACK);
        this.layoutInflater = layoutInflater;
        this.actionListener = actionListener;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public BlogPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_blog_post, parent, false);
        return new BlogPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogPostViewHolder holder, int position) {
        BlogPost post = getItem(position);
        holder.tvBlogTitle.setText(TextUtils.isEmpty(post.getTitle()) ? "" : post.getTitle());
        holder.tvBlogAuthor.setText(TextUtils.isEmpty(post.getAuthorName())
                ? holder.itemView.getContext().getString(R.string.unknown_user)
                : post.getAuthorName());
        holder.tvBlogPublishedAt.setText(formatTimestamp(post.getCreatedAt()));
        holder.tvBlogBody.setText(TextUtils.isEmpty(post.getBody()) ? "" : post.getBody());

        boolean isOwner = !TextUtils.isEmpty(currentUserUid) && currentUserUid.equals(post.getAuthorUid());
        holder.tvBlogOwnerBadge.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnDeleteBlogPost.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        holder.tvBlogAuthor.setOnClickListener(view -> actionListener.onViewProfile(post));
        holder.btnDeleteBlogPost.setOnClickListener(view -> actionListener.onDeletePost(post));
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return TIMESTAMP_FORMAT.format(timestamp.toDate());
    }

    static class BlogPostViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBlogTitle;
        private final TextView tvBlogAuthor;
        private final TextView tvBlogPublishedAt;
        private final TextView tvBlogOwnerBadge;
        private final TextView tvBlogBody;
        private final MaterialButton btnDeleteBlogPost;

        BlogPostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBlogTitle = itemView.findViewById(R.id.tvBlogTitle);
            tvBlogAuthor = itemView.findViewById(R.id.tvBlogAuthor);
            tvBlogPublishedAt = itemView.findViewById(R.id.tvBlogPublishedAt);
            tvBlogOwnerBadge = itemView.findViewById(R.id.tvBlogOwnerBadge);
            tvBlogBody = itemView.findViewById(R.id.tvBlogBody);
            btnDeleteBlogPost = itemView.findViewById(R.id.btnDeleteBlogPost);
        }
    }
}
