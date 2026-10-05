package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import com.sakibkhan.portfolio.service.ExternalPostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** The blog as visitors see it: published on-site posts and visible external ones, newest first. */
@Component
public class BlogEntries {

    private static final Comparator<BlogEntry> NEWEST_FIRST = Comparator.comparing(
            BlogEntry::publishedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

    private final PostRepository postRepository;
    private final ExternalPostService externalPostService;

    public BlogEntries(PostRepository postRepository, ExternalPostService externalPostService) {
        this.postRepository = postRepository;
        this.externalPostService = externalPostService;
    }

    public List<BlogEntry> all() {
        return Stream.concat(
                        postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED).stream().map(BlogEntry::of),
                        externalPostService.visible().stream().map(BlogEntry::of))
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /** The newest {@code count} across both kinds -- each side is asked for that many, since either could supply them all. */
    public List<BlogEntry> latest(int count) {
        return Stream.concat(
                        postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, PageRequest.of(0, count))
                                .stream().map(BlogEntry::of),
                        externalPostService.visible(count).stream().map(BlogEntry::of))
                .sorted(NEWEST_FIRST)
                .limit(count)
                .toList();
    }
}
