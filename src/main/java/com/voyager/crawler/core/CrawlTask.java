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

    public CrawlTask(URI uri, int depth, ContentFetcher fetcher, HtmlParser parser, ContentStorage storage) {
        this.uri = uri;
        this.depth = depth;
        this.fetcher = fetcher;
        this.parser = parser;
        this.storage = storage;
    }

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

            // Extract links for further processing
            return parser.extractLinks(uri, content);

        } catch (Exception e) {
            logger.error("Task failed for {}: {}", uri, e.getMessage());
            return Collections.emptySet();
        }
    }
}
