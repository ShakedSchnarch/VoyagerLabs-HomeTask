package com.voyager.crawler.util;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDedupService implements UrlDedupService {
    private final Set<String> visited = ConcurrentHashMap.newKeySet();

    @Override
    public boolean visit(URI uri) {
        if (uri == null)
            return false;
        // Normalize by toString to handle minor variations if needed,
        // but URI.equals handles parts. String is safer for dedup across slight
        // variations if we normalized earlier.
        // We assume input URIs are already normalized by UrlUtils.
        return visited.add(uri.toString());
    }

    @Override
    public int size() {
        return visited.size();
    }
}
