package com.voyager.crawler.core;

import com.voyager.crawler.config.CrawlerConfig;
import com.voyager.crawler.io.ContentFetcher;
import com.voyager.crawler.io.ContentStorage;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.util.ConcurrentDedupService;
import com.voyager.crawler.util.UrlDedupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CrawlerManagerTest {

    @Mock
    private ContentFetcher fetcher;
    @Mock
    private HtmlParser parser;
    @Mock
    private ContentStorage storage;

    private UrlDedupService dedupService;
    private CrawlerManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dedupService = new ConcurrentDedupService();
    }

    @Test
    void testSinglePageCrawl() throws Exception {
        URI seed = new URI("http://example.com");
        CrawlerConfig config = new CrawlerConfig(seed, 5, 0, true);

        when(fetcher.fetch(seed)).thenReturn(Optional.of("html"));
        when(parser.extractLinks(eq(seed), anyString())).thenReturn(Collections.emptySet());

        manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);
        manager.crawl();

        verify(fetcher, times(1)).fetch(seed);
        verify(storage, times(1)).save(eq(seed), anyString(), eq(0));
    }

    @Test
    void testBranchingFactorLimit() throws Exception {
        // Test that if a page returns 10 links, but maxLinksPerPage is 2, only 2 are
        // followed.
        URI seed = new URI("http://root.com");
        Set<URI> tenLinks = IntStream.range(0, 10)
                .mapToObj(i -> URI.create("http://child" + i + ".com"))
                .collect(Collectors.toSet());

        CrawlerConfig config = new CrawlerConfig(seed, 2, 1, true);

        when(fetcher.fetch(seed)).thenReturn(Optional.of("root"));
        when(parser.extractLinks(eq(seed), anyString())).thenReturn(tenLinks);

        // Mock children
        when(fetcher.fetch(any(URI.class))).thenReturn(Optional.of("child"));

        manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);
        manager.crawl();

        // Depth 0: 1 fetch (seed)
        // Depth 1: should be 2 fetches (limited by maxLinksPerPage=2)
        // Total fetches = 1 + 2 = 3.

        verify(fetcher, times(3)).fetch(any());
    }
}
