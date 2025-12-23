# Java 21 Web Crawler

A simple, multi-threaded web crawler built with Java 21 Virtual Threads (Project Loom).

## Key Features

* **Virtual Threads**: Uses `Executors.newVirtualThreadPerTaskExecutor()` to handle I/O efficiently without managing a complex thread pool.
* **BFS Traversal**: Crawls page by page in localized "waves" (depth 0, then depth 1, etc.).
* **Unique Filenames**: Saves pages as `<depth>/<clean_url>.html`, ensuring safe filenames for all OSs.
* **Politeness**: Checks `Content-Type` to download only HTML and adds a randomized delay between requests.
* **Resilience**: Retries failed requests with exponential backoff.

## How to Run

The easiest way to run the crawler is using the Gradle wrapper included in the project:

```bash
./gradlew run --args="https://news.ycombinator.com/ 5 2 true"
```

**Arguments:**

1. **Seed URL**: Where to start.
2. **Max Links**: Max links to extract per page (discovery order).
3. **Depth**: 0 = seed only, 1 = seed + immediate children.
4. **Unique**: `true` = globally unique (never revisit), `false` = unique per crawl level only.

**Note:**
* The crawler ignores `robots.txt` and does not render JavaScript.
* Filenames are sanitized and truncated to 200 characters for filesystem safety.

## Project Structure

* `CrawlerManager`: Main logic. Manages the crawl queue and shuts down gracefully.
* `ContentFetcher`: Wrapper around Java's `HttpClient` with retry logic.
* `HtmlParser`: Uses **Jsoup** to extracting links. Validates strictly (http/https).
* `LocalFileStorage`: Saves downloaded HTML to the `crawled_data/` directory.

## Testing

Access unit tests via:

```bash
./gradlew test
```

(Includes basic tests for URL sanitization and storage logic).
