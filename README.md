# Java 21 High-Performance Web Crawler

A robust, multi-threaded web crawler designed for high throughput and reliability. This project demonstrates modern Java concurrency patterns using **Virtual Threads** (Project Loom) and strictly adheres to **Design by Contract** principles.

## 🚀 Features

*   **Java 21 Virtual Threads**: Utilizes `Executors.newVirtualThreadPerTaskExecutor()` for blocking I/O operations without the overhead of OS threads.
*   **Design by Contract**: Core components (`ContentFetcher`, `HtmlParser`, `ContentStorage`) are defined with strict interfaces, preconditions, and postconditions.
*   **Branching Limit**: Configurable strict limit on the number of links extracted *per page* (Branching Factor), ensuring controlled growth.
*   **Resilience**: Smart retry mechanism with **Exponential Backoff** to handle `503 Service Unavailable` and `429 Too Many Requests`.
*   **Politeness**: Implements Jitter (randomized delay) to behave as a good citizen on the web.
*   **BFS "Wave" Algorithm**: Crawls in distinct depth layers, ensuring the "max depth" constraint is strictly observed.

## 🛠 Prerequisites

*   **Java 21 SDK** (Required for Virtual Threads)
*   **Gradle** (Wrapper or installed)

## 📦 Installation & Build

Clone the repository and build the project using Gradle:

```bash
git clone https://github.com/your-username/voyager-crawler.git
cd voyager-crawler
gradle build
```

To run the full suite of Unit Tests (JUnit 5 + Mockito):
```bash
gradle test
```

## 🏃 Usage

The application is a CLI tool accepting 4 arguments:
`java -jar crawler.jar <SeedURL> <MaxLinksPerPage> <Depth> <Unique>`

### Quick Run (via Gradle)

```bash
# Syntax: gradle run --args="<URL> <MaxLinks> <Depth> <IsUnique>"

gradle run --args="https://news.ycombinator.com/ 5 2 true"
```

*   **URL**: The starting point (Seed).
*   **MaxLinks**: The maximum number of new links to extract from *each* visited page.
*   **Depth**: How many levels deep to crawl (0 = only seed, 1 = seed + children, etc.).
*   **IsUnique**: Whether to enforce global uniqueness (visited pages are never re-visited).

### Output
Crawled pages are saved efficiently to local disk in the `crawled_data` directory, organized by depth:

```text
crawled_data/
├── 0/
│   └── news.ycombinator.com.html
├── 1/
│   ├── news.ycombinator.com_item_id_123.html
│   └── example.com_blog_post.html
```

## 🏗 Architecture

The system is decomposed into single-responsibility components:

*   **`CrawlerManager`**: The orchestration engine. It manages the BFS queues and submits tasks to the Virtual Thread executor.
*   **`ContentFetcher`**: Handles HTTP networking. Includes retry logic and politeness delays.
*   **`HtmlParser`**: Uses **Jsoup** to robustly parse HTML and extract canonical links.
*   **`ContentStorage`**: Persists content to disk. Filenames are sanitized for cross-platform compatibility.
*   **`UrlDedupService`**: A thread-safe service backed by `ConcurrentHashMap` to track visited URLs in O(1) time.

## 🧪 Testing

The project puts a strong emphasis on testability.
*   Network calls are abstracted behind `ContentFetcher`, allowing tests to run without internet access.
*   Unit tests verify edge cases like:
    *   Max depth cutoff.
    *   Cycle detection (visiting the same URL twice).
    *   Branching factor limits (e.g., page has 100 links, we only take 5).

---
*Submitted as a Home Assignment for Voyager Labs.*
