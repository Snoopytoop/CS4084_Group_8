package com.example.cs4084_group_8;

import android.text.TextUtils;

public final class BlogValidation {
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_BODY_LENGTH = 6000;

    private BlogValidation() {
    }

    public static String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    public static String normalizeBody(String body) {
        return body == null ? "" : body.trim();
    }

    public static boolean isTitleValid(String title) {
        String normalized = normalizeTitle(title);
        return !TextUtils.isEmpty(normalized) && normalized.length() <= MAX_TITLE_LENGTH;
    }

    public static boolean isBodyValid(String body) {
        String normalized = normalizeBody(body);
        return !TextUtils.isEmpty(normalized) && normalized.length() <= MAX_BODY_LENGTH;
    }
}
