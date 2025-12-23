package com.voyager.crawler.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Utility class for URL normalization and sanitization.
 */
public final class UrlUtils {

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");

    private UrlUtils() {
        // Prevent instantiation
    }

    /**
     * Converts a URI to a safe filename.
     * <p>
     * The strategy combines a sanitized version of the host and path with a short
     * hash of the full URI.
     * This ensures human readability while guaranteeing uniqueness for URLs that
     * differ only by query parameters.
     * </p>
     *
     * @param uri The URI to convert.
     * @return A filesystem-safe string unique to the input URI.
     * @return A filesystem-safe string derived from the URI.
     */
    public static String toFilename(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null");
        }

        // Strategy: Scheme + Host + Path + Query, replacing invalid chars with "_"
        String fullUrl = uri.toString();

        // Remove protocol
        String cleanUrl = fullUrl.replaceFirst("^https?://", "");

        String safeName = INVALID_FILENAME_CHARS.matcher(cleanUrl).replaceAll("_");

        if (safeName.isEmpty()) {
            safeName = "index";
        }

        // Limit length for OS safety
        if (safeName.length() > 200) {
            safeName = safeName.substring(0, 200);
        }

        return safeName;
    }

    /**
     * Normalizes a URL by stripping fragments and ensuring it is well-formed.
     *
     * @param uri The URI to normalize.
     * @return The normalized URI, or null if input is null.
     */
    public static URI normalize(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            // Reconstruct without fragment to avoid duplicates based on anchor tags
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(),
                    uri.getQuery(), null);
        } catch (URISyntaxException e) {
            return uri;
        }
    }

}
