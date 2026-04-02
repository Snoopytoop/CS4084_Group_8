package com.example.cs4084_group_8;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import java.io.IOException;
import java.util.Locale;

public final class ImageValidation {
    private ImageValidation() {
    }

    public static String validateImageSelection(Context context, Uri uri, long maxBytes) {
        if (uri == null) {
            return "No image selected.";
        }

        ContentResolver contentResolver = context.getContentResolver();
        String contentType = contentResolver.getType(uri);
        if (contentType == null || !contentType.startsWith("image/")) {
            return "Selected file must be an image.";
        }

        long sizeBytes = resolveSizeBytes(contentResolver, uri);
        if (sizeBytes < 0L) {
            return "Could not determine image size.";
        }
        if (sizeBytes > maxBytes) {
            return String.format(
                    Locale.US,
                    "Image must be %.0f MB or smaller.",
                    maxBytes / (1024f * 1024f)
            );
        }

        return null;
    }

    private static long resolveSizeBytes(ContentResolver contentResolver, Uri uri) {
        try (AssetFileDescriptor assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")) {
            if (assetFileDescriptor == null) {
                return -1L;
            }
            return assetFileDescriptor.getLength();
        } catch (IOException e) {
            return -1L;
        }
    }
}
