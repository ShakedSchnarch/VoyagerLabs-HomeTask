package com.voyager.crawler.util;

import java.net.*;
import java.util.regex.*;

/**
 * Utility helpers for URL normalization and filename sanitization.
 */
public final class UrlUtils {

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");

    private UrlUtils() {
    }

    /**
     * Builds a filesystem-safe filename from a URI.
     * Combines scheme, host, path, and query, and replaces invalid characters with {@code _}.
     *
     * @param uri the URI to convert.
     * @return a filesystem-safe string derived from the URI.
     * @throws IllegalArgumentException if {@code uri} is null.
     */
    public static String toFilename(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null");
        }

        String fullUrl = uri.toString();

        String cleanUrl = fullUrl.replace("://", "_");

        String safeName = INVALID_FILENAME_CHARS.matcher(cleanUrl).replaceAll("_");

        if (safeName.isEmpty()) {
            safeName = "index";
        }

        if (safeName.length() > 200) {
            safeName = safeName.substring(0, 200);
        }

        return safeName;
    }


    /**
     * Normalizes a URL by stripping fragments.
     *
     * @param uri the URI to normalize.
     * @return the normalized URI, or null if input is null.
     */
    public static URI normalize(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(),
                    uri.getQuery(), null);
        } catch (URISyntaxException e) {
            return uri;
        }
    }

}
