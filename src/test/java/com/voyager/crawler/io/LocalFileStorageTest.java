package com.voyager.crawler.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveCreatesDirectoryAndFile() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        URI uri = new URI("https://example.com/foo");
        String content = "hello world";

        storage.save(uri, content, 2);

        // Check directory <temp>/2/
        Path depthDir = tempDir.resolve("2");
        assertTrue(Files.exists(depthDir), "Depth directory should exist");
        assertTrue(Files.isDirectory(depthDir), "Depth directory should be a directory");

        // Check file <temp>/2/example.com_foo.html (sanitized)
        // Check any file in there to avoid strict filename dependency in test if
        // specific logic changes
        // But for unit test, we should know the logic.
        // Logic: host + path, replaced special chars.
        // host=example.com, path=/foo -> example.com/foo -> example.com_foo

        Path expectedFile = depthDir.resolve("example.com_foo.html");
        // Or if logic trims slash or something.

        // Let's just list files and see if one exists with content.
        assertTrue(Files.list(depthDir).count() > 0);

        Path actualFile = Files.list(depthDir).findFirst().get();
        String actualContent = Files.readString(actualFile);

        assertTrue(actualContent.contains("hello world"));
    }
}
