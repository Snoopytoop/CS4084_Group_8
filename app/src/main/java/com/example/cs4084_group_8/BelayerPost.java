package com.example.cs4084_group_8;

import org.json.JSONObject;

public class BelayerPost {
    private static final String KEY_POST_ID = "postId";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_WALL_NAME = "wallName";
    private static final String KEY_CLIMB_DAYS = "climbDays";
    private static final String KEY_CLIMB_TIMES = "climbTimes";
    private static final String KEY_BELAY_CAPABILITY = "belayCapability";
    private static final String KEY_CLIMB_CAPABILITY = "climbCapability";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_CONTACT_KEY = "contactKey";
    private static final String KEY_CREATED_AT = "createdAtMillis";

    private final String postId;
    private final String displayName;
    private final String wallName;
    private final String climbDays;
    private final String climbTimes;
    private final String belayCapability;
    private final String climbCapability;
    private final String notes;
    private final String contactKey;
    private final long createdAtMillis;

    public BelayerPost(
            String postId,
            String displayName,
            String wallName,
            String climbDays,
            String climbTimes,
            String belayCapability,
            String climbCapability,
            String notes,
            String contactKey,
            long createdAtMillis
    ) {
        this.postId = postId;
        this.displayName = displayName;
        this.wallName = wallName;
        this.climbDays = climbDays;
        this.climbTimes = climbTimes;
        this.belayCapability = belayCapability;
        this.climbCapability = climbCapability;
        this.notes = notes;
        this.contactKey = contactKey;
        this.createdAtMillis = createdAtMillis;
    }

    public String getPostId() {
        return postId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWallName() {
        return wallName;
    }

    public String getClimbDays() {
        return climbDays;
    }

    public String getClimbTimes() {
        return climbTimes;
    }

    public String getBelayCapability() {
        return belayCapability;
    }

    public String getClimbCapability() {
        return climbCapability;
    }

    public String getNotes() {
        return notes;
    }

    public String getContactKey() {
        return contactKey;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_POST_ID, postId);
            jsonObject.put(KEY_DISPLAY_NAME, displayName);
            jsonObject.put(KEY_WALL_NAME, wallName);
            jsonObject.put(KEY_CLIMB_DAYS, climbDays);
            jsonObject.put(KEY_CLIMB_TIMES, climbTimes);
            jsonObject.put(KEY_BELAY_CAPABILITY, belayCapability);
            jsonObject.put(KEY_CLIMB_CAPABILITY, climbCapability);
            jsonObject.put(KEY_NOTES, notes);
            jsonObject.put(KEY_CONTACT_KEY, contactKey);
            jsonObject.put(KEY_CREATED_AT, createdAtMillis);
        } catch (Exception ignored) {
            // Keep best-effort local persistence.
        }
        return jsonObject;
    }

    public static BelayerPost fromJson(JSONObject jsonObject) {
        String postId = jsonObject.optString(KEY_POST_ID, "").trim();
        String wallName = jsonObject.optString(KEY_WALL_NAME, "").trim();
        String climbDays = jsonObject.optString(KEY_CLIMB_DAYS, "").trim();
        String climbTimes = jsonObject.optString(KEY_CLIMB_TIMES, "").trim();
        String belayCapability = jsonObject.optString(KEY_BELAY_CAPABILITY, "").trim();
        String climbCapability = jsonObject.optString(KEY_CLIMB_CAPABILITY, "").trim();

        if (postId.isEmpty()
                || wallName.isEmpty()
                || climbDays.isEmpty()
                || climbTimes.isEmpty()
                || belayCapability.isEmpty()
                || climbCapability.isEmpty()) {
            return null;
        }

        String displayName = jsonObject.optString(KEY_DISPLAY_NAME, "");
        String notes = jsonObject.optString(KEY_NOTES, "");
        String contactKey = jsonObject.optString(KEY_CONTACT_KEY, "");
        long createdAtMillis = jsonObject.optLong(KEY_CREATED_AT, System.currentTimeMillis());

        return new BelayerPost(
                postId,
                displayName,
                wallName,
                climbDays,
                climbTimes,
                belayCapability,
                climbCapability,
                notes,
                contactKey,
                createdAtMillis
        );
    }
}
