package com.voyager.crawler.core;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.io.ContentFetcher;
import com.voyager.crawler.io.ContentStorage;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.util.UrlDedupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CrawlerManager {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerManager.class);

    private final CrawlerConfig config;
    private final ContentFetcher fetcher;
    private final HtmlParser parser;
    private final ContentStorage storage;
    private final UrlDedupService dedupService;
    private final ExecutorService executor;

    public CrawlerManager(CrawlerConfig config, ContentFetcher fetcher, HtmlParser parser, ContentStorage storage,
            UrlDedupService dedupService) {
        this.config = config;
        this.fetcher = fetcher;
        this.parser = parser;
        this.storage = storage;
        this.dedupService = dedupService;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void crawl() {
        logger.info("Starting crawl: {}", config);

        Set<URI> currentDepthUrls = new HashSet<>();
        if (config.isUnique()) {
            if (dedupService.visit(config.seedUrl())) {
                currentDepthUrls.add(config.seedUrl());
            }
        } else {
            currentDepthUrls.add(config.seedUrl());
        }

        int currentDepth = 0;

        // Loop depths
        while (currentDepth <= config.maxDepth() && !currentDepthUrls.isEmpty()) {
            logger.info("Processing Depth {}: {} URLs", currentDepth, currentDepthUrls.size());

            int finalDepth = currentDepth;

            // Submit all tasks for this wave
            List<CompletableFuture<Set<URI>>> futures = currentDepthUrls.stream()
                    .map(uri -> {
                        CrawlTask task = new CrawlTask(uri, finalDepth, fetcher, parser, storage);
                        return CompletableFuture.supplyAsync(task::call, executor);
                    })
                    .toList();

            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allDone.join();
            } catch (Exception e) {
                logger.error("Error waiting for wave completion", e);
            }

            // Collect results for NEXT wave
            if (currentDepth < config.maxDepth()) {
                Set<URI> nextDepthUrls = Collections.synchronizedSet(new HashSet<>());

                // Process each result set individually to apply the 'Per Page' limit
                futures.forEach(f -> {
                    try {
                        Set<URI> links = f.join(); // Result from one page

                        // Apply Branching Factor Limit HERE
                        links.stream()
                                .limit(config.maxLinksPerPage()) // "maximal number of different URLs to extract from
                                                                 // the page"
                                .forEach(link -> {
                                    // Check global uniqueness / add to next wave
                                    if (config.isUnique()) {
                                        if (dedupService.visit(link)) {
                                            nextDepthUrls.add(link);
                                        }
                                    } else {
                                        nextDepthUrls.add(link);
                                    }
                                });

                    } catch (Exception e) {
                        logger.warn("Failed to get results from a task", e);
                    }
                });

                currentDepthUrls = nextDepthUrls;
            } else {
                currentDepthUrls = Collections.emptySet();
            }

            currentDepth++;
        }

        // More accurate logging
        logger.info("Crawl finished. Total pages visited (Persisted): {}", dedupService.size());
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
