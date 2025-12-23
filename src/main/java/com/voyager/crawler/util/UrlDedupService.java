package com.voyager.crawler.util;

import java.net.URI;

public interface UrlDedupService {
    /**
     * Checks if the URI has been visited and marks it as visited if not.
     * 
     * @param uri The URI to check.
     * @return true if the URI was NOT previously visited (i.e., it's new), false
     *         otherwise.
     */
    boolean visit(URI uri);

    /**
     * Returns the count of visited URLs.
     */
    int size();
}
