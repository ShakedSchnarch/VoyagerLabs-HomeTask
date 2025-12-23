package com.voyager.crawler.io;

import java.net.URI;
import java.util.Optional;

public interface ContentFetcher {
    /**
     * Fetches the raw HTML content of a URL.
     * 
     * @param uri the URI to fetch.
     * @return Optional containing the HTML body, or empty if fetch failed (graceful
     *         degradation).
     * @throws NullPointerException if uri is null (Precondition).
     */
    Optional<String> fetch(URI uri);
}
