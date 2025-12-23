package com.voyager.crawler.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class JavaHttpClientFetcher implements ContentFetcher {
    private static final Logger logger = LoggerFactory.getLogger(JavaHttpClientFetcher.class);
    private static final int MAX_RETRIES = 3;

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
        while (attempt <= MAX_RETRIES) {
            try {
                // Politeness delay inside the fetching thread (Virtual threads make this cheap)
                // Random jitter to avoid thundering herd if many threads start exactly together
                long delay = 50 + (long) (Math.random() * 100);
                if (attempt > 0) {
                    delay = (long) Math.pow(2, attempt) * 500; // Exponential backoff: 1s, 2s, 4s...
                    logger.info("Retrying {} in {}ms (Attempt {})", uri, delay, attempt + 1);
                }
                Thread.sleep(delay);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "VoyagerCrawler/1.0 (Education Project)") // Clean UA
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return Optional.of(response.body());
                } else if (status == 429 || status == 503 || status == 500 || status == 502) {
                    // Retryable errors
                    logger.warn("Fetch failed for URI: {}. Status code: {}. Retrying...", uri, status);
                    attempt++;
                } else {
                    // Non-retryable (404, 403, etc.)
                    logger.warn("Fetch failed for URI: {}. Status code: {}. Aborting task.", uri, status);
                    return Optional.empty();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Fetch interrupted for URI: {}", uri);
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Error fetching URI: {}. Error: {}", uri, e.getMessage());
                attempt++; // Retry on IO exceptions too
            }
        }

        logger.error("Giving up on URI: {} after {} attempts", uri, MAX_RETRIES);
        return Optional.empty();
    }
}
