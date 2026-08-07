package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import com.sakibkhan.portfolio.service.MarkdownService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BlogController {

    private final PostRepository postRepository;
    private final MarkdownService markdownService;

    public BlogController(PostRepository postRepository, MarkdownService markdownService) {
        this.postRepository = postRepository;
        this.markdownService = markdownService;
    }

    @GetMapping("/blog")
    public String list(Model model) {
        model.addAttribute("posts", postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED));
        return "blog/list";
    }

    @GetMapping("/blog/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        model.addAttribute("post", post);
        model.addAttribute("contentHtml", markdownService.render(post.getContentMarkdown()));
        return "blog/detail";
    }
}
