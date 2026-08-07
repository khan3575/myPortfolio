package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostAdminService {

    private final PostRepository postRepository;

    public PostAdminService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(Post post) {
        post.setId(null);
        post.setSlug(resolveSlug(post));
        return postRepository.save(post);
    }

    public Post update(Long id, Post form) {
        Post existing = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        existing.setTitle(form.getTitle());
        existing.setSummary(form.getSummary());
        existing.setContentMarkdown(form.getContentMarkdown());
        if (form.getSlug() != null && !form.getSlug().isBlank() && !form.getSlug().equals(existing.getSlug())) {
            existing.setSlug(dedupeSlug(slugify(form.getSlug()), existing.getId()));
        }
        return postRepository.save(existing);
    }

    public void publish(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        post.setStatus(PostStatus.PUBLISHED);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        postRepository.save(post);
    }

    public void unpublish(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        post.setStatus(PostStatus.DRAFT);
        postRepository.save(post);
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    private String resolveSlug(Post post) {
        String base = (post.getSlug() != null && !post.getSlug().isBlank())
                ? slugify(post.getSlug())
                : slugify(post.getTitle());
        return dedupeSlug(base, null);
    }

    private String dedupeSlug(String base, Long ignoreId) {
        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, ignoreId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, Long ignoreId) {
        return postRepository.findBySlug(slug)
                .map(existing -> !existing.getId().equals(ignoreId))
                .orElse(false);
    }

    private String slugify(String input) {
        String slug = input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "post" : slug;
    }
}
