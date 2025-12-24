package com.voyager.crawler.util;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Thread-safe implementation of {@link UrlDedupService} backed by a concurrent set.
 * Assumes input URIs are already normalized by {@link UrlUtils}.
 */
public class ConcurrentDedupService implements UrlDedupService {
    private final Set<String> visited = ConcurrentHashMap.newKeySet();

    @Override
    public boolean visit(URI uri) {
        if (uri == null)
            return false;
        return visited.add(uri.toString());
    }

    @Override
    public int size() {
        return visited.size();
    }
}
