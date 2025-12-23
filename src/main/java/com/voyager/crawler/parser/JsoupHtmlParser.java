package com.voyager.crawler.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.voyager.crawler.util.UrlUtils;

public class JsoupHtmlParser implements HtmlParser {
    private static final Logger logger = LoggerFactory.getLogger(JsoupHtmlParser.class);

    @Override
    public Set<URI> extractLinks(URI baseUri, String html) {
        Objects.requireNonNull(baseUri, "baseUri must not be null");
        Objects.requireNonNull(html, "html must not be null");

        Set<URI> links = new HashSet<>();
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
                    logger.trace("Skipping malformed URL: {}", absUrl);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse HTML from {}", baseUri, e);
        }

        return links;
    }
}
