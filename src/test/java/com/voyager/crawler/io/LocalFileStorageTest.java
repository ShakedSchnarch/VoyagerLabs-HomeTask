package com.voyager.crawler.io;

import com.voyager.crawler.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

import java.net.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LocalFileStorage}.
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveCreatesDirectoryAndFile() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        URI uri = new URI("https://example.com/foo");
        String content = "hello world";

        storage.save(uri, content, 2);

        Path depthDir = tempDir.resolve("2");
        assertTrue(Files.isDirectory(depthDir), "Depth directory should be a directory");

        Path expectedFile = depthDir.resolve(UrlUtils.toFilename(uri) + ".html");
        assertTrue(Files.exists(expectedFile), "Expected file should exist");

        String actualContent = Files.readString(expectedFile);
        assertTrue(actualContent.contains(content));
    }
}
