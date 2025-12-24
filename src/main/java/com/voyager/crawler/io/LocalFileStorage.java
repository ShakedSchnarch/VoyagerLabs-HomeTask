package com.voyager.crawler.io;

import com.voyager.crawler.util.UrlUtils;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/**
 * Implementation of {@link ContentStorage} that saves pages to the local filesystem.
 * Directory structure: {@code <root>/<depth>/<safe_filename>}.
 */
public class LocalFileStorage implements ContentStorage {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorage.class);
    private final Path rootDir;

    public LocalFileStorage(String rootPath) {
        this.rootDir = Paths.get(rootPath);
    }

    @Override
    public void save(URI uri, String content, int depth) {
        Objects.requireNonNull(uri, "uri must not be null");
        Objects.requireNonNull(content, "content must not be null");

        try {
            Path depthDir = rootDir.resolve(String.valueOf(depth));
            if (!Files.exists(depthDir)) {
                Files.createDirectories(depthDir);
            }

            String filename = UrlUtils.toFilename(uri) + ".html";
            Path filePath = depthDir.resolve(filename);

            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved {} to {}", uri, filePath);

        } catch (IOException e) {
            logger.error("Failed to save content for {}: {}", uri, e.getMessage());
            throw new RuntimeException("Storage failure", e);
        }
    }
}
