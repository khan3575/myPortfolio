package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.ExternalPost;
import com.sakibkhan.portfolio.model.ExternalPostSource;
import com.sakibkhan.portfolio.repository.ExternalPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The articles the blog links out to. They arrive two ways -- pulled from the
 * Medium feed on a schedule, or typed in on the admin page -- and the rules
 * for how those two coexist live here: the feed keeps its own rows current
 * until an admin edits one, after which that row is the admin's.
 */
@Service
public class ExternalPostService {

    private static final Logger log = LoggerFactory.getLogger(ExternalPostService.class);

    /** Match the column widths in {@code external_posts}. */
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 1000;

    /** What a sync did, for the admin page to report back. */
    public record SyncResult(int added, int updated, int unchanged) {}

    private final ExternalPostRepository externalPostRepository;
    private final MediumFeedClient mediumFeedClient;

    public ExternalPostService(ExternalPostRepository externalPostRepository, MediumFeedClient mediumFeedClient) {
        this.externalPostRepository = externalPostRepository;
        this.mediumFeedClient = mediumFeedClient;
    }

    public List<ExternalPost> visible() {
        return externalPostRepository.findByHiddenFalseOrderByPublishedAtDesc();
    }

    public List<ExternalPost> visible(int limit) {
        return externalPostRepository.findByHiddenFalseOrderByPublishedAtDesc(PageRequest.of(0, limit));
    }

    public List<ExternalPost> all() {
        return externalPostRepository.findAllByOrderByPublishedAtDesc();
    }

    public Optional<ExternalPost> find(Long id) {
        return externalPostRepository.findById(id);
    }

    /**
     * The first run waits a minute so start-up never depends on Medium. A
     * failure costs nothing but a log line: the cards already stored keep
     * being served, and the next hour tries again.
     */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void scheduledSync() {
        if (!mediumFeedClient.isConfigured()) {
            return;
        }
        try {
            SyncResult result = syncFromMedium();
            if (result.added() > 0 || result.updated() > 0) {
                log.info("Medium sync: {} added, {} updated", result.added(), result.updated());
            }
        } catch (RuntimeException e) {
            log.warn("Medium sync failed, keeping the posts already stored: {}", e.getMessage());
        }
    }

    /**
     * Brings the stored Medium posts in line with the feed. A post that has
     * dropped out of the feed is left alone -- the feed only holds the ten
     * most recent, so absence there says nothing about the article.
     *
     * @throws IllegalStateException with a message meant for the admin screen
     */
    public SyncResult syncFromMedium() {
        if (!mediumFeedClient.isConfigured()) {
            throw new IllegalStateException("No Medium feed is configured (MEDIUM_FEED_URL is blank).");
        }

        int added = 0;
        int updated = 0;
        int unchanged = 0;
        for (MediumFeedClient.FeedItem item : mediumFeedClient.fetch()) {
            if (item.url().length() > MAX_URL_LENGTH) {
                continue;
            }
            Optional<ExternalPost> existing = externalPostRepository.findByUrl(item.url());
            if (existing.isEmpty()) {
                externalPostRepository.save(ExternalPost.builder()
                        .source(ExternalPostSource.MEDIUM)
                        .url(item.url())
                        .title(item.title())
                        .summary(item.summary())
                        .imageUrl(item.imageUrl())
                        .publishedAt(item.publishedAt())
                        .build());
                added++;
            } else if (applyFeedItem(existing.get(), item)) {
                externalPostRepository.save(existing.get());
                updated++;
            } else {
                unchanged++;
            }
        }
        return new SyncResult(added, updated, unchanged);
    }

    /** Copies the feed's version over an unlocked row; false if there was nothing to change. */
    private boolean applyFeedItem(ExternalPost post, MediumFeedClient.FeedItem item) {
        if (post.isLocked()) {
            return false;
        }
        boolean same = Objects.equals(post.getTitle(), item.title())
                && Objects.equals(post.getSummary(), item.summary())
                && Objects.equals(post.getImageUrl(), item.imageUrl())
                && Objects.equals(post.getPublishedAt(), item.publishedAt());
        if (same) {
            return false;
        }
        post.setTitle(item.title());
        post.setSummary(item.summary());
        post.setImageUrl(item.imageUrl());
        post.setPublishedAt(item.publishedAt());
        return true;
    }

    /**
     * @throws IllegalArgumentException with a message meant for the admin screen
     */
    public ExternalPost addManual(String url, String title, String summary, String imageUrl, LocalDate publishedOn) {
        String cleanUrl = requireWebUrl(url, "Article link");
        if (externalPostRepository.findByUrl(cleanUrl).isPresent()) {
            throw new IllegalArgumentException("That article is already listed.");
        }
        return externalPostRepository.save(ExternalPost.builder()
                .source(ExternalPostSource.MANUAL)
                .url(cleanUrl)
                .title(requireTitle(title))
                .summary(cleanSummary(summary))
                .imageUrl(cleanImageUrl(imageUrl))
                .publishedAt(publishedOn == null ? LocalDateTime.now() : publishedOn.atStartOfDay())
                // Nothing syncs a manual row, but locking it keeps that true even
                // if its link later turns up in the feed.
                .locked(true)
                .build());
    }

    /**
     * An admin edit. Locks the row, so the next sync doesn't put the feed's
     * wording straight back.
     *
     * @throws IllegalArgumentException with a message meant for the admin screen
     */
    public void update(Long id, String title, String summary, String imageUrl, LocalDate publishedOn) {
        ExternalPost post = require(id);
        post.setTitle(requireTitle(title));
        post.setSummary(cleanSummary(summary));
        post.setImageUrl(cleanImageUrl(imageUrl));
        if (publishedOn != null && !publishedOn.equals(post.getPublishedAt().toLocalDate())) {
            post.setPublishedAt(publishedOn.atStartOfDay());
        }
        post.setLocked(true);
        externalPostRepository.save(post);
    }

    public void setHidden(Long id, boolean hidden) {
        ExternalPost post = require(id);
        post.setHidden(hidden);
        externalPostRepository.save(post);
    }

    /** Hands a Medium row back to the feed; the next sync restores Medium's title, text and image. */
    public void unlock(Long id) {
        ExternalPost post = require(id);
        if (post.getSource() != ExternalPostSource.MEDIUM) {
            throw new IllegalArgumentException("Only posts imported from Medium can be reset to the feed.");
        }
        post.setLocked(false);
        externalPostRepository.save(post);
    }

    /**
     * Manual entries only. A Medium post that is still in the feed would be
     * imported again within the hour, so those are hidden instead.
     */
    public void delete(Long id) {
        ExternalPost post = require(id);
        if (post.getSource() == ExternalPostSource.MEDIUM) {
            throw new IllegalArgumentException("Posts imported from Medium come back on the next sync. Hide it instead.");
        }
        externalPostRepository.delete(post);
    }

    private ExternalPost require(Long id) {
        return externalPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That post no longer exists."));
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A title is required.");
        }
        String clean = title.strip();
        if (clean.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Titles are capped at " + MAX_TITLE_LENGTH + " characters.");
        }
        return clean;
    }

    private String cleanSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return null;
        }
        String clean = summary.strip();
        if (clean.length() > MAX_SUMMARY_LENGTH) {
            throw new IllegalArgumentException("Summaries are capped at " + MAX_SUMMARY_LENGTH + " characters.");
        }
        return clean;
    }

    private String cleanImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : requireWebUrl(imageUrl, "Image link");
    }

    /**
     * These end up in href and src attributes on public pages, so anything
     * that isn't plainly a web address -- "javascript:" above all -- is refused.
     */
    private String requireWebUrl(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String clean = value.strip();
        if (clean.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        try {
            URI uri = URI.create(clean);
            boolean web = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
            if (!web || uri.getHost() == null) {
                throw new IllegalArgumentException(label + " must be a full http(s) address.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(label + " must be a full http(s) address.");
        }
        return clean;
    }
}
