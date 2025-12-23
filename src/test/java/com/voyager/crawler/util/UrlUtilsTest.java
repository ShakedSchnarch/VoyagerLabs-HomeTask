package com.voyager.crawler.util;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class UrlUtilsTest {

    @Test
    void testToFilename_Uniqueness() throws Exception {
        URI uri1 = new URI("https://example.com/page?id=1");
        URI uri2 = new URI("https://example.com/page?id=2");

        String filename1 = UrlUtils.toFilename(uri1);
        String filename2 = UrlUtils.toFilename(uri2);

        assertNotEquals(filename1, filename2, "Filenames should differ due to different query params");
        assertTrue(filename1.startsWith("example_com_page"), "Filename should contain readable prefix");
    }

    @Test
    void testToFilename_Sanitization() throws Exception {
        // Use a valid URI that contains characters we definitely want to replace in a
        // filename (like / and ?)
        URI uri = new URI("https://example.com/foo/bar?q=value");
        String filename = UrlUtils.toFilename(uri);

        // The slash '/' and question mark '?' should be replaced by underscores
        assertFalse(filename.contains("/"), "Slash should be replaced");
        assertFalse(filename.contains("?"), "Question mark should be replaced");
        assertTrue(filename.contains("_"), "Separators should use underscore");

        // Verify structure survives (host_path...)
        String[] parts = filename.split("_");
        assertTrue(parts.length >= 2, "Should have multiple parts due to replacements");
    }

    @Test
    void testNormalize_RemovesFragment() throws Exception {
        URI uri = new URI("https://example.com/page#section1");
        URI normalized = UrlUtils.normalize(uri);

        assertEquals("https://example.com/page", normalized.toString());
    }
}
