package com.voyager.crawler.core;

import com.voyager.crawler.io.ContentFetcher;
import com.voyager.crawler.io.ContentStorage;
import com.voyager.crawler.parser.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A Callable task responsible for processing a single URL.
 * Steps: Fetch -> Save -> Extract Links.
 */
public class CrawlTask implements Callable<Set<URI>> {
    private static final Logger logger = LoggerFactory.getLogger(CrawlTask.class);

    private final URI uri;
    private final int depth;
    private final ContentFetcher fetcher;
    private final HtmlParser parser;
    private final ContentStorage storage;
    private final boolean extractLinks;

    /**
     * Creates a crawling task for a single URI.
     *
     * @param uri           the target URI.
     * @param depth         the crawl depth for the URI.
     * @param fetcher       component responsible for fetching content.
     * @param parser        HTML parser used for link extraction.
     * @param storage       storage backend for saving fetched pages.
     * @param extractLinks  flag indicating whether to extract links from the
     *                      fetched content.
     */
    public CrawlTask(URI uri, int depth, ContentFetcher fetcher, HtmlParser parser, ContentStorage storage,
            boolean extractLinks) {
        this.uri = uri;
        this.depth = depth;
        this.fetcher = fetcher;
        this.parser = parser;
        this.storage = storage;
        this.extractLinks = extractLinks;
    }

    /**
     * Fetches, stores, and optionally parses the URI content for outbound links.
     *
     * @return a set of discovered links, or an empty set if extraction is skipped
     *         or fails.
     */
    @Override
    public Set<URI> call() {
        if (logger.isDebugEnabled()) {
            logger.debug("Starting task for {} at depth {}", uri, depth);
        }

        try {
            var contentOpt = fetcher.fetch(uri);
            if (contentOpt.isEmpty()) {
                return Collections.emptySet();
            }
            String content = contentOpt.get();

            storage.save(uri, content, depth);

            if (!extractLinks) {
                return Collections.emptySet();
            }

            return parser.extractLinks(uri, content);

        } catch (Exception e) {
            logger.error("Task failed for {}: {}", uri, e.getMessage());
            return Collections.emptySet();
        }
    }
}
