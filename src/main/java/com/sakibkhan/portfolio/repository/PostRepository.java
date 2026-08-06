package com.sakibkhan.portfolio.repository;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** Public blog list — published posts only, newest first. */
    List<Post> findByStatusOrderByPublishedAtDesc(PostStatus status);

    /** Public post detail — a draft's slug must not resolve for a visitor. */
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    /** Admin edit lookup — needs to find drafts too. */
    Optional<Post> findBySlug(String slug);

    /** Admin listing — every post regardless of status, newest first. */
    List<Post> findAllByOrderByCreatedAtDesc();

    boolean existsBySlug(String slug);
}
