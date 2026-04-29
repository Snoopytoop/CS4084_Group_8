package com.example.cs4084_group_8;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    public interface PostActionListener {
        void onLikeClick(Post post);

        void onCommentClick(Post post);
    }

    private final List<Post> posts = new ArrayList<>();
    private final String currentUserUid;
    private final PostActionListener listener;

    public PostAdapter(String currentUserUid, PostActionListener listener) {
        this.currentUserUid = currentUserUid;
        this.listener = listener;
    }

    public void submitList(List<Post> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        holder.tvPostAuthor.setText(TextUtils.isEmpty(post.getAuthorName())
                ? holder.itemView.getContext().getString(R.string.unknown_user)
                : post.getAuthorName());
        String authorUid = post.getAuthorUid();
        if (TextUtils.isEmpty(authorUid)) {
            holder.tvPostAuthor.setOnClickListener(null);
            holder.tvPostAuthor.setClickable(false);
            holder.tvPostAuthor.setEnabled(false);
        } else {
            holder.tvPostAuthor.setEnabled(true);
            holder.tvPostAuthor.setClickable(true);
            holder.tvPostAuthor.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), UserProfileActivity.class);
                intent.putExtra("USER_ID", authorUid);
                v.getContext().startActivity(intent);
            });
        }
        holder.tvPostContent.setText(TextUtils.isEmpty(post.getContent()) ? "" : post.getContent());
        holder.btnLike.setText(holder.itemView.getContext().getString(R.string.post_like_format, post.getLikesCount()));
        holder.btnComment.setText(holder.itemView.getContext().getString(R.string.post_comment_format, post.getCommentsCount()));

        boolean likedByCurrentUser = post.getLikedBy().contains(currentUserUid);
        holder.btnLike.setEnabled(!TextUtils.isEmpty(currentUserUid));
        holder.btnLike.setAlpha(likedByCurrentUser ? 0.7f : 1.0f);

        String postType = post.getPostType() == null ? "" : post.getPostType().trim().toLowerCase(Locale.US);
        String mediaUrl = post.getMediaUrl();
        if ("image".equals(postType) && !TextUtils.isEmpty(mediaUrl)) {
            holder.ivPostMedia.setVisibility(View.VISIBLE);
            holder.tvMediaHint.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(mediaUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.ivPostMedia);
        } else if ("video".equals(postType) && !TextUtils.isEmpty(mediaUrl)) {
            holder.ivPostMedia.setVisibility(View.GONE);
            holder.tvMediaHint.setVisibility(View.VISIBLE);
            holder.tvMediaHint.setText(holder.itemView.getContext().getString(R.string.post_video_url_format, mediaUrl));
        } else {
            holder.ivPostMedia.setVisibility(View.GONE);
            holder.tvMediaHint.setVisibility(View.GONE);
        }

        holder.btnLike.setOnClickListener(v -> listener.onLikeClick(post));
        holder.btnComment.setOnClickListener(v -> listener.onCommentClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostAuthor;
        TextView tvPostContent;
        TextView tvMediaHint;
        ImageView ivPostMedia;
        MaterialButton btnLike;
        MaterialButton btnComment;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostAuthor = itemView.findViewById(R.id.tvPostAuthor);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvMediaHint = itemView.findViewById(R.id.tvMediaHint);
            ivPostMedia = itemView.findViewById(R.id.ivPostMedia);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}
