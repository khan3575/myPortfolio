package com.sakibkhan.portfolio.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing only -- no Spring context and no network, against a feed shaped like Medium's. */
class MediumFeedClientTest {

    private static List<MediumFeedClient.FeedItem> sample() throws IOException {
        try (InputStream in = MediumFeedClientTest.class.getResourceAsStream("/medium-feed-sample.xml")) {
            return MediumFeedClient.parse(in.readAllBytes());
        }
    }

    @Test
    void readsTitleLinkAndDate() throws IOException {
        MediumFeedClient.FeedItem first = sample().get(0);

        assertEquals("How the JVM Runs Your Code & Why It Matters", first.title());
        // The "?source=rss-..." tracking suffix must not become part of the post's identity.
        assertEquals("https://medium.com/@example/how-the-jvm-runs-your-code-1a2b3c", first.url());
        assertEquals(LocalDateTime.of(2026, 10, 1, 7, 26), first.publishedAt());
    }

    @Test
    void takesTheCoverImageAndSummarizesTheBody() throws IOException {
        MediumFeedClient.FeedItem first = sample().get(0);

        assertEquals("https://cdn-images-1.medium.com/max/1024/cover.png", first.imageUrl());
        assertTrue(first.summary().startsWith("Hi, I’m an example author."), first.summary());
        assertTrue(first.summary().endsWith("…"), first.summary());
        assertTrue(first.summary().length() <= 201, first.summary());
        assertFalse(first.summary().contains("caption"), "figure captions are not the opening lines");
        assertFalse(first.summary().contains("<"), "no markup survives into the summary");
    }

    @Test
    void ignoresTheTrackingPixelWhenAnArticleHasNoPicture() throws IOException {
        MediumFeedClient.FeedItem second = sample().get(1);

        assertNull(second.imageUrl());
        assertEquals("Just a few words.", second.summary());
    }

    @Test
    void skipsEntriesThatCannotMakeACard() throws IOException {
        assertEquals(2, sample().size());
    }

    @Test
    void refusesADoctype() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss><channel><item><title>&xxe;</title></item></channel></rss>
                """;
        assertThrows(IllegalStateException.class, () -> MediumFeedClient.parse(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
