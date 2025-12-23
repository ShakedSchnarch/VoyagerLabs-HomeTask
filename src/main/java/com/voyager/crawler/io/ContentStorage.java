package com.voyager.crawler.io;

import java.net.URI;

public interface ContentStorage {
    /**
     * Saves the content to a file structure.
     * 
     * @param uri     the source URI (used for naming).
     * @param content the HTML content.
     * @param depth   the current depth level (used for directory structure).
     * @throws NullPointerException if inputs are null (Precondition).
     * @throws RuntimeException     runtime wrapper for IO issues (Postcondition).
     */
    void save(URI uri, String content, int depth);
}
