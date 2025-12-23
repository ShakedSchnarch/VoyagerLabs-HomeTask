package com.voyager.crawler.parser;

import java.net.URI;
import java.util.Set;

public interface HtmlParser {
    /**
     * Extracts all unique hyperlinks from the html content.
     * 
     * @param baseUri the base URI to resolve relative links.
     * @param html    the raw HTML content.
     * @return A set of valid, absolute URIs.
     * @throws NullPointerException if baseUri or html is null (Precondition).
     */
    Set<URI> extractLinks(URI baseUri, String html);
}
