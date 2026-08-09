package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class PostAdminService {

    // Bounded so a persistently-taken base slug can't loop forever; five
    // collisions in a row on a single-admin blog would mean something else
    // is wrong.
    private static final int MAX_SLUG_ATTEMPTS = 5;

    private final PostRepository postRepository;

    public PostAdminService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(Post post) {
        post.setId(null);
        String base = (post.getSlug() != null && !post.getSlug().isBlank())
                ? slugify(post.getSlug())
                : slugify(post.getTitle());
        return saveWithUniqueSlug(post, base);
    }

    public Post update(Long id, Post form) {
        Post existing = findOrNotFound(id);
        existing.setTitle(form.getTitle());
        existing.setSummary(form.getSummary());
        existing.setContentMarkdown(form.getContentMarkdown());

        if (form.getSlug() != null && !form.getSlug().isBlank()) {
            String base = slugify(form.getSlug());
            if (!base.equals(existing.getSlug())) {
                return saveWithUniqueSlug(existing, base);
            }
        }
        return postRepository.save(existing);
    }

    public void publish(Long id) {
        Post post = findOrNotFound(id);
        post.setStatus(PostStatus.PUBLISHED);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        postRepository.save(post);
    }

    public void unpublish(Long id) {
        Post post = findOrNotFound(id);
        post.setStatus(PostStatus.DRAFT);
        postRepository.save(post);
    }

    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        postRepository.deleteById(id);
    }

    private Post findOrNotFound(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Saves with a slug derived from {@code base}, retrying with an
     * incremented suffix on a unique-constraint conflict. The earlier
     * check-then-save approach (look up "is this slug taken?", then save
     * separately) raced under concurrent creates of the same title -- two
     * requests could both see the slug as free before either had saved.
     * Retrying on the save's own {@link DataIntegrityViolationException}
     * closes that window: the database's unique constraint is the actual
     * source of truth, not a query that can go stale between check and
     * write.
     */
    private Post saveWithUniqueSlug(Post post, String base) {
        String candidate = base;
        int suffix = 2;
        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            post.setSlug(candidate);
            try {
                return postRepository.saveAndFlush(post);
            } catch (DataIntegrityViolationException e) {
                candidate = base + "-" + suffix++;
            }
        }
        throw new IllegalStateException("Could not generate a unique slug for: " + base);
    }

    private String slugify(String input) {
        String slug = input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "post" : slug;
    }
}
