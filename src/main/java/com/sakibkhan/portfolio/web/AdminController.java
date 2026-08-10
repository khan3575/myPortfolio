package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.repository.PostRepository;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import com.sakibkhan.portfolio.service.PostAdminService;
import com.sakibkhan.portfolio.service.ProjectAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PostRepository postRepository;
    private final PostAdminService postAdminService;
    private final ProjectRepository projectRepository;
    private final ProjectAdminService projectAdminService;

    public AdminController(
            PostRepository postRepository,
            PostAdminService postAdminService,
            ProjectRepository projectRepository,
            ProjectAdminService projectAdminService
    ) {
        this.postRepository = postRepository;
        this.postAdminService = postAdminService;
        this.projectRepository = projectRepository;
        this.projectAdminService = projectAdminService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("projects", projectRepository.findAllByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }

    @PostMapping("/projects/github")
    public String addProjectFromGitHub(
            @RequestParam String repoUrl,
            @RequestParam(required = false) String liveUrl,
            Model model
    ) {
        try {
            projectAdminService.addFromGitHub(repoUrl, liveUrl);
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("projectRepoUrlInput", repoUrl);
            model.addAttribute("projectLiveUrlInput", liveUrl);
            return dashboardWithProjectError(model, e.getMessage());
        }
    }

    @PostMapping("/projects/manual")
    public String addProjectManually(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String liveUrl,
            Model model
    ) {
        try {
            projectAdminService.addManual(name, description, language, liveUrl);
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("projectNameInput", name);
            model.addAttribute("projectDescriptionInput", description);
            model.addAttribute("projectLanguageInput", language);
            model.addAttribute("projectManualLiveUrlInput", liveUrl);
            return dashboardWithProjectError(model, e.getMessage());
        }
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id) {
        projectAdminService.delete(id);
        return "redirect:/admin";
    }

    private String dashboardWithProjectError(Model model, String error) {
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("projects", projectRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("projectError", error);
        return "admin/dashboard";
    }

    @GetMapping("/posts/new")
    public String newPost(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("formAction", "/admin/posts");
        return "admin/post-form";
    }

    @PostMapping("/posts")
    public String create(@ModelAttribute Post post, Model model) {
        if (!isValid(post)) {
            return invalidForm(post, "/admin/posts", model);
        }
        postAdminService.create(post);
        return "redirect:/admin";
    }

    @GetMapping("/posts/{id}/edit")
    public String editPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        model.addAttribute("formAction", "/admin/posts/" + id);
        return "admin/post-form";
    }

    @PostMapping("/posts/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Post post, Model model) {
        if (!isValid(post)) {
            return invalidForm(post, "/admin/posts/" + id, model);
        }
        postAdminService.update(id, post);
        return "redirect:/admin";
    }

    @PostMapping("/posts/{id}/publish")
    public String publish(@PathVariable Long id) {
        postAdminService.publish(id);
        return "redirect:/admin";
    }

    @PostMapping("/posts/{id}/unpublish")
    public String unpublish(@PathVariable Long id) {
        postAdminService.unpublish(id);
        return "redirect:/admin";
    }

    @GetMapping("/posts/{id}/delete")
    public String confirmDelete(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        return "admin/delete-confirm";
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {
        postAdminService.delete(id);
        return "redirect:/admin";
    }

    private boolean isValid(Post post) {
        return post.getTitle() != null && !post.getTitle().isBlank()
                && post.getContentMarkdown() != null && !post.getContentMarkdown().isBlank();
    }

    private String invalidForm(Post post, String formAction, Model model) {
        model.addAttribute("post", post);
        model.addAttribute("formAction", formAction);
        model.addAttribute("error", "Title and content are required.");
        return "admin/post-form";
    }
}
