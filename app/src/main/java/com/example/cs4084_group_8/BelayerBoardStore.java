package com.example.cs4084_group_8;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BelayerBoardStore {
    private static final String PREFS_NAME = "belayer_board_prefs";
    private static final String KEY_POSTS = "belayer_posts_v1";

    private final SharedPreferences sharedPreferences;

    public BelayerBoardStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<BelayerPost> getAllPosts() {
        List<BelayerPost> posts = new ArrayList<>();
        String payload = sharedPreferences.getString(KEY_POSTS, "[]");
        if (payload == null || payload.trim().isEmpty()) {
            return posts;
        }

        try {
            JSONArray jsonArray = new JSONArray(payload);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                if (jsonObject == null) {
                    continue;
                }

                BelayerPost post = BelayerPost.fromJson(jsonObject);
                if (post != null) {
                    posts.add(post);
                }
            }
        } catch (Exception ignored) {
            // Keep an empty board if local data is invalid.
        }

        posts.sort(Comparator.comparingLong(BelayerPost::getCreatedAtMillis).reversed());
        return posts;
    }

    public void addPost(BelayerPost post) {
        List<BelayerPost> posts = getAllPosts();
        posts.add(0, post);
        savePosts(posts);
    }

    private void savePosts(List<BelayerPost> posts) {
        JSONArray jsonArray = new JSONArray();
        for (BelayerPost post : posts) {
            jsonArray.put(post.toJson());
        }

        sharedPreferences.edit()
                .putString(KEY_POSTS, jsonArray.toString())
                .apply();
    }
}
