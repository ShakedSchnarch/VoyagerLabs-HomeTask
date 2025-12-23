package com.voyager.crawler.core;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.io.*;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.util.UrlDedupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerManager {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerManager.class);

    private final CrawlerConfig config;
    private final ContentFetcher fetcher;
    private final HtmlParser parser;
    private final ContentStorage storage;
    private final UrlDedupService dedupService;
    private final ExecutorService executor;

    private final AtomicInteger pagesSaved = new AtomicInteger(0);

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

            // Submit tasks for this wave
            List<CompletableFuture<Set<URI>>> futures = currentDepthUrls.stream()
                    .map(uri -> {
                        CrawlTask task = new CrawlTask(uri, finalDepth, fetcher, parser, storage);
                        return CompletableFuture.supplyAsync(task::call, executor)
                                .thenApply(links -> {
                                    pagesSaved.incrementAndGet();
                                    return links;
                                });
                    })
                    .toList();

            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allDone.join();
            } catch (Exception e) {
                logger.error("Error waiting for depth {} completion", currentDepth, e);
            }

            // Collect results for NEXT wave
            if (currentDepth < config.maxDepth()) {
                // No need for synchronization here as we process sequentially in this thread
                Set<URI> nextDepthUrls = new HashSet<>();

                // Process each result set individually
                futures.forEach(f -> {
                    try {
                        Set<URI> links = f.join(); // Result from one page

                        links.stream()
                                .limit(config.maxLinksPerPage())
                                .forEach(link -> {
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

        logger.info("Crawl finished. Total pages visited (Persisted): {}", dedupService.size());
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
