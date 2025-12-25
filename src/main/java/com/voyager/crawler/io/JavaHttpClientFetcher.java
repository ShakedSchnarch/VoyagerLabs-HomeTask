package com.voyager.crawler.io;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.voyager.crawler.util.ConsolePrinter;

/**
 * Implementation of {@link ContentFetcher} using Java's {@link HttpClient}.
 * Adds exponential backoff and a small politeness delay between requests.
 */
public class JavaHttpClientFetcher implements ContentFetcher {
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 50;

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
                            return Optional.empty();
                        }

                        String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
                        return Optional.of(body);

                    }

                    if (isRetryable(status)) {
                        ConsolePrinter.warn(
                                "Fetch failed for URI: " + uri + ". Status code: " + status + ". Retrying...");
                        attempt++;
                        continue;
                    }

                    ConsolePrinter.warn(
                            "Fetch failed for URI: " + uri + ". Status code: " + status + ". Aborting task.");
                    return Optional.empty();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ConsolePrinter.warn("Fetch interrupted for URI: " + uri);
                return Optional.empty();
            } catch (Exception e) {
                ConsolePrinter.error("Error fetching URI: " + uri + ". Error: " + e);
                attempt++;
            }
        }

        ConsolePrinter.error("Dropping URL after " + MAX_RETRIES + " attempts: " + uri);
        return Optional.empty();
    }

    private void applyPolitenessDelay(int attempt, URI uri) throws InterruptedException {
        long delay = BASE_DELAY_MS + (long) (Math.random() * 100);
        if (attempt > 0) {
            delay = (long) Math.pow(2, attempt) * 500; // Exponential backoff
        }
        Thread.sleep(delay);
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 503 || status == 500 || status == 502;
    }
}
