package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.ExternalPost;
import com.sakibkhan.portfolio.service.ExternalPostService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * The admin side of the articles the blog links out to. Outcomes go back to
 * the dashboard as flash messages, so a refresh of that page doesn't repeat
 * the action that produced them.
 */
@Controller
@RequestMapping("/admin/external-posts")
public class ExternalPostAdminController {

    private static final String DASHBOARD = "redirect:/admin#external-posts";

    private final ExternalPostService externalPostService;

    public ExternalPostAdminController(ExternalPostService externalPostService) {
        this.externalPostService = externalPostService;
    }

    @PostMapping("/refresh")
    public String refresh(RedirectAttributes redirect) {
        try {
            ExternalPostService.SyncResult result = externalPostService.syncFromMedium();
            redirect.addFlashAttribute("externalPostNotice", "Medium checked: %d added, %d updated, %d unchanged."
                    .formatted(result.added(), result.updated(), result.unchanged()));
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("externalPostError", e.getMessage());
        }
        return DASHBOARD;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, null, "/admin/external-posts", null, null, null, null, LocalDate.now());
    }

    @PostMapping
    public String create(
            @RequestParam String url,
            @RequestParam String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedOn,
            Model model
    ) {
        try {
            externalPostService.addManual(url, title, summary, imageUrl, publishedOn);
            return DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return form(model, null, "/admin/external-posts", url, title, summary, imageUrl, publishedOn);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ExternalPost post = require(id);
        return form(model, post, "/admin/external-posts/" + id, post.getUrl(), post.getTitle(), post.getSummary(),
                post.getImageUrl(), post.getPublishedAt().toLocalDate());
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedOn,
            Model model
    ) {
        ExternalPost post = require(id);
        try {
            externalPostService.update(id, title, summary, imageUrl, publishedOn);
            return DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return form(model, post, "/admin/external-posts/" + id, post.getUrl(), title, summary, imageUrl, publishedOn);
        }
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id, RedirectAttributes redirect) {
        return act(redirect, () -> externalPostService.setHidden(id, true));
    }

    @PostMapping("/{id}/show")
    public String show(@PathVariable Long id, RedirectAttributes redirect) {
        return act(redirect, () -> externalPostService.setHidden(id, false));
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes redirect) {
        return act(redirect, () -> externalPostService.unlock(id));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        return act(redirect, () -> externalPostService.delete(id));
    }

    private String act(RedirectAttributes redirect, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("externalPostError", e.getMessage());
        }
        return DASHBOARD;
    }

    private ExternalPost require(Long id) {
        return externalPostService.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** {@code post} is null when adding; the field values are passed separately so a rejected form keeps what was typed. */
    private String form(Model model, ExternalPost post, String formAction, String url, String title, String summary,
                        String imageUrl, LocalDate publishedOn) {
        model.addAttribute("externalPost", post);
        model.addAttribute("formAction", formAction);
        model.addAttribute("urlInput", url);
        model.addAttribute("titleInput", title);
        model.addAttribute("summaryInput", summary);
        model.addAttribute("imageUrlInput", imageUrl);
        model.addAttribute("publishedOnInput", publishedOn);
        return "admin/external-post-form";
    }
}
