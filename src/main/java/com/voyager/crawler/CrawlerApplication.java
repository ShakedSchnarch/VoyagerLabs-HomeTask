package com.voyager.crawler;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.core.CrawlerManager;
import com.voyager.crawler.io.ContentFetcher;
import com.voyager.crawler.io.ContentStorage;
import com.voyager.crawler.io.JavaHttpClientFetcher;
import com.voyager.crawler.io.LocalFileStorage;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.parser.JsoupHtmlParser;
import com.voyager.crawler.util.ConcurrentDedupService;
import com.voyager.crawler.util.UrlDedupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;

public class CrawlerApplication {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerApplication.class);

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                System.out.println("Usage: java -jar crawler.jar <seedUrl> <maxLinksPerPage> <maxDepth> <isUnique>");
                System.out.println("Wait! I will run a default demo for you because arguments are missing.");
                // Fallback for easy "gradle run" without args
                runCrawler(new URI("https://news.ycombinator.com/"), 5, 2, true);
                return;
            }

            URI seedUrl = new URI(args[0]);
            int maxLinksPerPage = Integer.parseInt(args[1]);
            int maxDepth = Integer.parseInt(args[2]);
            boolean isUnique = Boolean.parseBoolean(args[3]);

            runCrawler(seedUrl, maxLinksPerPage, maxDepth, isUnique);

        } catch (Exception e) {
            logger.error("Application failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void runCrawler(URI seed, int maxLinks, int depth, boolean unique) {
        logger.info("Running with: Seed={}, MaxLinksPerPage={}, Depth={}, Unique={}", seed, maxLinks, depth, unique);

        CrawlerConfig config = new CrawlerConfig(seed, maxLinks, depth, unique);

        ContentFetcher fetcher = new JavaHttpClientFetcher();
        HtmlParser parser = new JsoupHtmlParser();
        ContentStorage storage = new LocalFileStorage("crawled_data");
        UrlDedupService dedupService = new ConcurrentDedupService();

        CrawlerManager manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);

        long start = System.currentTimeMillis();
        manager.crawl();
        long end = System.currentTimeMillis();

        logger.info("Total processing time: {} ms", (end - start));
        manager.shutdown();
    }
}
