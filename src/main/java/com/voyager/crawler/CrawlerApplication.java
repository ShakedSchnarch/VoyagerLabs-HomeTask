package com.voyager.crawler;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.core.CrawlerManager;
import com.voyager.crawler.io.*;
import com.voyager.crawler.parser.*;
import com.voyager.crawler.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Main entry point for the Voyager Crawler.
 */
public class CrawlerApplication {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerApplication.class);

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                printUsage();
                System.exit(1);
            }

            URI seedUrl = new URI(args[0]);
            int maxLinksPerPage = Integer.parseInt(args[1]);
            int maxDepth = Integer.parseInt(args[2]);
            boolean isUnique = Boolean.parseBoolean(args[3]);

            runCrawler(seedUrl, maxLinksPerPage, maxDepth, isUnique);

        } catch (Exception e) {
            logger.error("Application failed: {}", e.getMessage(), e);
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java -jar crawler.jar <seedUrl> <maxLinksPerPage> <maxDepth> <isUnique>

                Arguments:
                  seedUrl          - The starting URL (e.g., https://example.com)
                  maxLinksPerPage  - Maximum number of links to follow from each page
                  maxDepth         - Traversal depth (0 = only seed)
                  isUnique         - true for global uniqueness, false for per-level uniqueness

                Example:
                  java -jar crawler.jar https://www.ynetnews.com 5 2 true
                """);
    }

    private static void runCrawler(URI seed, int maxLinks, int depth, boolean unique) {
        logger.info("Initializing Crawler with: Seed={}, MaxLinks={}, Depth={}, Unique={}", seed, maxLinks, depth,
                unique);

        CrawlerConfig config = new CrawlerConfig(seed, maxLinks, depth, unique);

        // Dependency Injection wiring
        ContentFetcher fetcher = new JavaHttpClientFetcher();
        HtmlParser parser = new JsoupHtmlParser();
        ContentStorage storage = new LocalFileStorage("crawled_data");
        UrlDedupService dedupService = new ConcurrentDedupService();

        CrawlerManager manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);

        long start = System.currentTimeMillis();
        try {
            manager.crawl();
        } finally {
            manager.shutdown();
        }
        long end = System.currentTimeMillis();

        logger.info("Crawl completed in {} ms", (end - start));
    }
}
