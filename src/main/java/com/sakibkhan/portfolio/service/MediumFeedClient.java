package com.sakibkhan.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the Medium RSS feed. The feed, not the article pages: Medium answers
 * a server asking for an article with a 403, but serves the feed, and the feed
 * carries everything a card needs -- title, link, date and the full body, from
 * which the preview image and text are taken.
 */
@Component
class MediumFeedClient {

    private static final String CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/";
    private static final int SUMMARY_LENGTH = 200;
    private static final int MAX_TITLE_LENGTH = 255;

    private static final Pattern IMAGE_TAG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_SRC = Pattern.compile("\\bsrc=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    /** Captions belong to their picture, not to the opening lines of the article. */
    private static final Pattern FIGURE = Pattern.compile("(?is)<figure\\b.*?</figure>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    /** One feed entry, reduced to what a card shows. */
    record FeedItem(String title, String url, LocalDateTime publishedAt, String imageUrl, String summary) {}

    private final RestClient restClient;
    private final String feedUrl;

    MediumFeedClient(RestClient.Builder restClientBuilder, @Value("${medium.feed-url}") String feedUrl) {
        // Bounded, so a hung connection to Medium ties up the scheduler thread
        // for seconds rather than for good.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "sakib-khan.com feed reader (+https://sakib-khan.com)")
                .build();
        this.feedUrl = feedUrl.strip();
    }

    boolean isConfigured() {
        return !feedUrl.isEmpty();
    }

    /**
     * @throws IllegalStateException if the feed can't be fetched or isn't the XML expected
     */
    List<FeedItem> fetch() {
        byte[] xml;
        try {
            xml = restClient.get().uri(feedUrl).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Could not reach the Medium feed: " + e.getMessage(), e);
        }
        if (xml == null || xml.length == 0) {
            throw new IllegalStateException("The Medium feed came back empty.");
        }
        return parse(xml);
    }

    /** Separate from the fetch so it can be tested against a saved feed. */
    static List<FeedItem> parse(byte[] xml) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // The feed is someone else's XML: no DOCTYPE means no external
            // entities, which is the whole XXE family ruled out at once.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IllegalStateException("The Medium feed is not valid XML.", e);
        }

        List<FeedItem> items = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            FeedItem item = toItem((Element) nodes.item(i));
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** Null for an entry missing any of the three things a card can't do without. */
    private static FeedItem toItem(Element item) {
        String title = childText(item, null, "title");
        String link = childText(item, null, "link");
        LocalDateTime publishedAt = parseDate(childText(item, null, "pubDate"));
        if (title == null || link == null || publishedAt == null) {
            return null;
        }

        String body = childText(item, CONTENT_NAMESPACE, "encoded");
        if (body == null) {
            body = childText(item, null, "description");
        }
        if (body == null) {
            body = "";
        }

        return new FeedItem(
                truncate(title, MAX_TITLE_LENGTH),
                stripQuery(link),
                publishedAt,
                firstImage(body),
                summarize(body));
    }

    private static String childText(Element parent, String namespace, String localName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE || !localName.equals(child.getLocalName())) {
                continue;
            }
            boolean sameNamespace = namespace == null
                    ? child.getNamespaceURI() == null
                    : namespace.equals(child.getNamespaceURI());
            if (sameNamespace) {
                String text = child.getTextContent().strip();
                return text.isEmpty() ? null : text;
            }
        }
        return null;
    }

    /** Feed dates are RFC 1123 in GMT; stored as UTC to line up with the on-site posts' timestamps. */
    private static LocalDateTime parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Medium appends "?source=rss-..." to every link. Dropping it keeps the
     * URL stable, which matters because the URL is what identifies the post.
     */
    private static String stripQuery(String link) {
        int query = link.indexOf('?');
        return query < 0 ? link : link.substring(0, query);
    }

    /**
     * The first real picture in the article. Medium ends every body with a
     * 1x1 tracking pixel, which would otherwise be "the image" of any article
     * that has none of its own.
     */
    private static String firstImage(String body) {
        Matcher tags = IMAGE_TAG.matcher(body);
        while (tags.find()) {
            String tag = tags.group();
            Matcher src = IMAGE_SRC.matcher(tag);
            if (!src.find()) {
                continue;
            }
            String url = HtmlUtils.htmlUnescape(src.group(1));
            if (url.startsWith("https://") && !url.contains("/_/stat") && !tag.contains("width=\"1\"")) {
                return url;
            }
        }
        return null;
    }

    /** The article's opening lines as plain text, cut at a word. */
    private static String summarize(String body) {
        String withoutFigures = FIGURE.matcher(body).replaceAll(" ");
        String text = HtmlUtils.htmlUnescape(TAG.matcher(withoutFigures).replaceAll(" "))
                .replaceAll("\\s+", " ")
                .strip();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() <= SUMMARY_LENGTH) {
            return text;
        }
        int cut = text.lastIndexOf(' ', SUMMARY_LENGTH);
        return text.substring(0, cut > 0 ? cut : SUMMARY_LENGTH).replaceAll("[\\s.,;:—–-]+$", "") + "…";
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
