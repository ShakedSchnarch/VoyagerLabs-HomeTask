package com.voyager.crawler.config;

import java.net.URI;
import java.util.Objects;

/**
 * Configuration for the Web Crawler.
 *
 * @param seedUrl         The starting URL for the crawl.
 * @param maxLinksPerPage The maximal number of different URLs to extract from
 *                        EACH page.
 * @param maxDepth        The maximum depth of the crawl (0-indexed).
 * @param isUnique        Whether to enforce uniqueness of visited URLs
 *                        globally.
 */
public record CrawlerConfig(URI seedUrl, int maxLinksPerPage, int maxDepth, boolean isUnique) {
    public CrawlerConfig {
        Objects.requireNonNull(seedUrl, "seedUrl must not be null");
        if (maxLinksPerPage < 0) {
            throw new IllegalArgumentException("maxLinksPerPage must be non-negative");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must be non-negative");
        }
    }
}
