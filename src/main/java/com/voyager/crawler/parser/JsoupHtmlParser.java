package com.voyager.crawler.parser;

import com.voyager.crawler.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.net.*;
import java.util.*;

/**
 * Implementation of {@link HtmlParser} using Jsoup.
 * Extracts absolute, normalized HTTP(S) links from HTML content.
 */
public class JsoupHtmlParser implements HtmlParser {
    @Override
    public Set<URI> extractLinks(URI baseUri, String html) {
        Objects.requireNonNull(baseUri, "baseUri must not be null");
        Objects.requireNonNull(html, "html must not be null");

        Set<URI> links = new LinkedHashSet<>();
        try {
            Document doc = Jsoup.parse(html, baseUri.toString());
            Elements anchors = doc.select("a[href]");

            for (Element anchor : anchors) {
                String absUrl = anchor.attr("abs:href");
                if (absUrl.isEmpty())
                    continue;

                try {
                    URI uri = URI.create(absUrl);
                    URI normalized = UrlUtils.normalize(uri);
                    if (normalized != null
                            && (normalized.getScheme().equals("http") || normalized.getScheme().equals("https"))) {
                        links.add(normalized);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore malformed URLs
                }
            }
        } catch (Exception e) {
            ConsolePrinter.error("Failed to parse HTML from " + baseUri + ": " + e);
        }

        return links;
    }
}
