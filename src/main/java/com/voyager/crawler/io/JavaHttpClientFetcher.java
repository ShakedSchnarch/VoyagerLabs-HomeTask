package com.voyager.crawler.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of ContentFetcher using Java's native HttpClient (available
 * since Java 11).
 * Supports exponential backoff and basic politeness delays.
 */
public class JavaHttpClientFetcher implements ContentFetcher {
    private static final Logger logger = LoggerFactory.getLogger(JavaHttpClientFetcher.class);
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 50;

    // Configurable timeout
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client;

    public JavaHttpClientFetcher() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public Optional<String> fetch(URI uri) {
        Objects.requireNonNull(uri, "URI must not be null");

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                applyPolitenessDelay(attempt, uri);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .timeout(TIMEOUT)
                        .header("User-Agent", "VoyagerCrawler/1.0 (Student Project)")
                        .build();

                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();

                try (InputStream bodyStream = response.body()) {
                    if (status >= 200 && status < 300) {
                        // Check Content-Type
                        Optional<String> contentTypeOpt = response.headers().firstValue("Content-Type");
                        if (contentTypeOpt.isPresent() && !contentTypeOpt.get().toLowerCase().contains("text/html")) {
                            logger.debug("Skipping non-HTML content: {} ({})", uri, contentTypeOpt.get());
                            return Optional.empty();
                        }

                        String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
                        return Optional.of(body);

                    }

                    if (isRetryable(status)) {
                        logger.warn("Fetch failed for URI: {}. Status code: {}. Retrying...", uri, status);
                        attempt++;
                        continue;
                    }

                    logger.warn("Fetch failed for URI: {}. Status code: {}. Aborting task.", uri, status);
                    return Optional.empty();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Fetch interrupted for URI: {}", uri);
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Error fetching URI: {}. Error: {}", uri, e.getMessage());
                attempt++;
            }
        }

        logger.error("Dropping URL after {} attempts: {}", MAX_RETRIES, uri);
        return Optional.empty();
    }

    private void applyPolitenessDelay(int attempt, URI uri) throws InterruptedException {
        long delay = BASE_DELAY_MS + (long) (Math.random() * 100);
        if (attempt > 0) {
            delay = (long) Math.pow(2, attempt) * 500; // Exponential backoff
            logger.debug("Retrying {} in {}ms (Attempt {})", uri, delay, attempt + 1);
        }
        Thread.sleep(delay);
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 503 || status == 500 || status == 502;
    }
}
