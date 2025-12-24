package com.voyager.crawler.core;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.io.*;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.util.UrlDedupService;
import com.voyager.crawler.util.UrlUtils;
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
    // Limit concurrent I/O operations to avoid "Too many open files" and generic
    // 429s.
    // Virtual threads are cheap, but sockets/descriptors are not.
    private final Semaphore rateLimiter = new Semaphore(50);

    /**
     * Constructs a new CrawlerManager.
     * <p>
     * Uses {@link Executors#newVirtualThreadPerTaskExecutor()} for high-throughput
     * I/O-bound tasks testing.
     * This avoids the overhead of platform threads while maintaining a simple
     * thread-per-task coding style.
     * </p>
     *
     * @param config       Configuration parameters.
     * @param fetcher      Component to fetch web content.
     * @param parser       Component to parse HTML.
     * @param storage      Component to save content to disk.
     * @param dedupService Component to manage visited URLs.
     */
    public CrawlerManager(CrawlerConfig config, ContentFetcher fetcher, HtmlParser parser, ContentStorage storage,
            UrlDedupService dedupService) {
        this.config = config;
        this.fetcher = fetcher;
        this.parser = parser;
        this.storage = storage;
        this.dedupService = dedupService;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executes the crawl from the configured seed URL, enforcing depth and branching
     * limits while optionally deduplicating URLs.
     */
    public void crawl() {
        logger.info("Starting crawl: {}", config);
        URI seed = UrlUtils.normalize(config.seedUrl());
        if (seed == null) {
            logger.warn("Seed URL is null after normalization. Aborting crawl.");
            return;
        }

        Set<URI> currentDepthUrls = new HashSet<>();
        if (config.isUnique()) {
            if (dedupService.visit(seed)) {
                currentDepthUrls.add(seed);
            }
        } else {
            currentDepthUrls.add(seed);
        }

        int currentDepth = 0;

        // Loop depths
        while (currentDepth <= config.maxDepth() && !currentDepthUrls.isEmpty()) {
            logger.info("Processing Depth {}: {} URLs", currentDepth, currentDepthUrls.size());

            int finalDepth = currentDepth;
            boolean shouldExtractLinks = finalDepth < config.maxDepth();

            // Submit tasks for this wave
            List<CompletableFuture<Set<URI>>> futures = currentDepthUrls.stream()
                    .map(uri -> {
                        CrawlTask task = new CrawlTask(uri, finalDepth, fetcher, parser, storage, shouldExtractLinks,
                                pagesSaved);
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                rateLimiter.acquire();
                                try {
                                    return task.call();
                                } finally {
                                    rateLimiter.release();
                                }
                            } catch (Exception e) {
                                if (e instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                }
                                throw new CompletionException(e);
                            }
                        }, executor);
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
                Set<URI> nextDepthUrls = new LinkedHashSet<>();

                // Process each result set individually
                futures.forEach(f -> {
                    try {
                        Set<URI> links = f.join(); // Result from one page

                        links.stream()
                                .filter(link -> !config.isUnique() || dedupService.visit(link))
                                .limit(config.maxLinksPerPage())
                                .forEach(nextDepthUrls::add);

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

        logger.info("Crawl finished. Total pages successfully processed: {}", pagesSaved.get());
    }

    /**
     * Shuts down the executor service backing the crawler, waiting for tasks to
     * complete and forcing termination if needed.
     */
    public void shutdown() {
        if (executor != null) {
            logger.info("Shutting down crawler executor...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in time, forcing shutdown...");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warn("Shutdown interrupted, forcing shutdown...");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
