package com.voyager.crawler.io;

import com.voyager.crawler.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

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
