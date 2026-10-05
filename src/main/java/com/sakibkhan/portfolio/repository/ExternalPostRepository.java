package com.sakibkhan.portfolio.repository;

import com.sakibkhan.portfolio.model.ExternalPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalPostRepository extends JpaRepository<ExternalPost, Long> {

    /** Public blog list — everything not hidden, newest first. */
    List<ExternalPost> findByHiddenFalseOrderByPublishedAtDesc();

    /** Homepage preview — same query, limited at the database. */
    List<ExternalPost> findByHiddenFalseOrderByPublishedAtDesc(Pageable pageable);

    /** Admin listing — hidden ones included. */
    List<ExternalPost> findAllByOrderByPublishedAtDesc();

    /** How a feed item, or a link typed in twice, finds the row it already has. */
    Optional<ExternalPost> findByUrl(String url);
}
