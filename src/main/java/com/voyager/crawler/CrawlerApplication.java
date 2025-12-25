package com.voyager.crawler;

import com.voyager.crawler.config.*;
import com.voyager.crawler.core.*;
import com.voyager.crawler.io.*;
import com.voyager.crawler.parser.*;
import com.voyager.crawler.util.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * CLI entry point for the Voyager crawler.
 */
public class CrawlerApplication {
    private static final String OUTPUT_BASE_DIR = "crawled_data";
    private static final String OUTPUT_DIR_PREFIX = "crawler_output_";
    private static final DateTimeFormatter OUTPUT_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * CLI entry point for running the crawler.
     *
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        try {
            CliArguments cli = parseArguments(args);

            String outputDirName = buildOutputDirName();
            Path outputDir = Paths.get(outputDirName).toAbsolutePath().normalize();

            printBanner(cli.seedUrl(), cli.maxDepth(), cli.maxLinksPerPage(), cli.isUnique(), outputDir);

            CrawlerConfig config = new CrawlerConfig(cli.seedUrl(), cli.maxLinksPerPage(), cli.maxDepth(),
                    cli.isUnique());

            ContentFetcher fetcher = new JavaHttpClientFetcher();
            HtmlParser parser = new JsoupHtmlParser();
            ContentStorage storage = new LocalFileStorage(outputDirName);
            UrlDedupService dedupService = new ConcurrentDedupService();

            CrawlerManager manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);

            long startTimeNs = System.nanoTime();
            try {
                manager.crawl();
            } finally {
                manager.shutdown();
            }
            long durationMs = (System.nanoTime() - startTimeNs) / 1_000_000;
            printSummary(durationMs, outputDir);

        } catch (NumberFormatException e) {
            printError(e.getMessage());
            printUsage();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            printError("Unexpected error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static CliArguments parseArguments(String[] args) {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException("Expected 4 arguments.");
        }

        String seedArg = args[0].trim();
        if (seedArg.isEmpty()) {
            throw new IllegalArgumentException("seedUrl must not be empty.");
        }
        if (!seedArg.startsWith("http://") && !seedArg.startsWith("https://")) {
            throw new IllegalArgumentException("seedUrl must start with http:// or https://");
        }
        URI seedUrl = URI.create(seedArg);

        int maxLinksPerPage = parseNonNegativeInt(args[1], "maxLinksPerPage");
        int maxDepth = parseNonNegativeInt(args[2], "maxDepth");
        boolean isUnique = parseBooleanStrict(args[3], "isUnique");

        return new CliArguments(seedUrl, maxLinksPerPage, maxDepth, isUnique);
    }

    private static int parseNonNegativeInt(String value, String name) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new NumberFormatException(name + " must be an integer.");
        }

        int parsed;
        try {
            parsed = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(name + " must be an integer.");
        }

        if (parsed < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
        return parsed;
    }

    private static boolean parseBooleanStrict(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must be 'true' or 'false'.");
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be 'true' or 'false'.");
    }

    private static String buildOutputDirName() {
        String timestamp = OUTPUT_DIR_FORMATTER.format(LocalDateTime.now());
        return Paths.get(OUTPUT_BASE_DIR, OUTPUT_DIR_PREFIX + timestamp).toString();
    }

    private static void printBanner(URI seedUrl, int maxDepth, int maxLinksPerPage, boolean isUnique,
            Path outputDir) {
        ConsolePrinter.info("Voyager Crawler");
        ConsolePrinter.info("----------------");
        ConsolePrinter.infoKeyValue("Seed URL:", seedUrl);
        ConsolePrinter.infoKeyValue("Max Depth:", maxDepth);
        ConsolePrinter.infoKeyValue("Max Links/Page:", maxLinksPerPage);
        ConsolePrinter.infoKeyValue("Unique:", isUnique);
        ConsolePrinter.infoKeyValue("Output Directory:", outputDir);
        ConsolePrinter.blankLine();
    }

    private static void printSummary(long durationMs, Path outputDir) {
        double durationSeconds = durationMs / 1000.0;
        String durationText = String.format(Locale.US, "%.2f s (%d ms)", durationSeconds, durationMs);
        ConsolePrinter.info("Results:");
        ConsolePrinter.info("Crawl complete.");
        ConsolePrinter.info("Total execution time: " + durationText);
        ConsolePrinter.info("Output directory: " + outputDir);
    }

    private static void printError(String message) {
        if (message == null || message.isBlank()) {
            ConsolePrinter.error("Invalid arguments.");
            return;
        }
        ConsolePrinter.error(message);
    }

    private static void printUsage() {
        ConsolePrinter.info("Usage: java -jar crawler.jar <seedUrl> <maxLinksPerPage> <maxDepth> <isUnique>");
        ConsolePrinter.blankLine();
        ConsolePrinter.info("Arguments:");
        ConsolePrinter.info("  seedUrl          - The starting URL (e.g., https://example.com)");
        ConsolePrinter.info("  maxLinksPerPage  - Maximum number of links to follow from each page");
        ConsolePrinter.info("  maxDepth         - Traversal depth (0 = only seed)");
        ConsolePrinter.info("  isUnique         - true for global uniqueness, false for per-level uniqueness");
        ConsolePrinter.blankLine();
        ConsolePrinter.info("Example:");
        ConsolePrinter.info("  java -jar crawler.jar https://www.ynetnews.com 5 2 true");
    }

    private record CliArguments(URI seedUrl, int maxLinksPerPage, int maxDepth, boolean isUnique) {
    }
}
