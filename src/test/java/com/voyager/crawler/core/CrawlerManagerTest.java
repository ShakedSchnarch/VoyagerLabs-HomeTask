package com.voyager.crawler.core;

import com.voyager.crawler.config.*;
import com.voyager.crawler.io.*;
import com.voyager.crawler.parser.HtmlParser;
import com.voyager.crawler.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.net.*;
import java.util.*;
import java.util.stream.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CrawlerManager} with mocked IO dependencies.
 */
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
        URI seed = new URI("http://root.com");
        Set<URI> tenLinks = IntStream.range(0, 10)
                .mapToObj(i -> URI.create("http://child" + i + ".com"))
                .collect(Collectors.toSet());

        CrawlerConfig config = new CrawlerConfig(seed, 2, 1, true);

        when(fetcher.fetch(seed)).thenReturn(Optional.of("root"));
        when(parser.extractLinks(eq(seed), anyString())).thenReturn(tenLinks);

        when(fetcher.fetch(any(URI.class))).thenReturn(Optional.of("child"));

        manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);
        manager.crawl();

        verify(fetcher, times(3)).fetch(any());
    }

    @Test
    void testAllowsRevisitWhenNotUnique() throws Exception {
        URI seed = new URI("http://example.com");
        CrawlerConfig config = new CrawlerConfig(seed, 5, 1, false);

        when(fetcher.fetch(seed)).thenReturn(Optional.of("root"));
        when(parser.extractLinks(eq(seed), anyString())).thenReturn(Set.of(seed));

        manager = new CrawlerManager(config, fetcher, parser, storage, dedupService);
        manager.crawl();

        verify(fetcher, times(2)).fetch(seed);
    }
}
