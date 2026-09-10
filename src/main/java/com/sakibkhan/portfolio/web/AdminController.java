package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.repository.PostRepository;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import com.sakibkhan.portfolio.service.CvService;
import com.sakibkhan.portfolio.service.ImageStorageService;
import com.sakibkhan.portfolio.service.PostAdminService;
import com.sakibkhan.portfolio.service.ProjectAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PostRepository postRepository;
    private final PostAdminService postAdminService;
    private final ProjectRepository projectRepository;
    private final ProjectAdminService projectAdminService;
    private final ImageStorageService imageStorageService;
    private final CvService cvService;

    public AdminController(
            PostRepository postRepository,
            PostAdminService postAdminService,
            ProjectRepository projectRepository,
            ProjectAdminService projectAdminService,
            ImageStorageService imageStorageService,
            CvService cvService
    ) {
        this.postRepository = postRepository;
        this.postAdminService = postAdminService;
        this.projectRepository = projectRepository;
        this.projectAdminService = projectAdminService;
        this.imageStorageService = imageStorageService;
        this.cvService = cvService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        return populatedDashboard(model);
    }

    private String populatedDashboard(Model model) {
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("projects", projectRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("cv", cvService.currentSummary().orElse(null));
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
            @RequestParam(required = false) String languages,
            @RequestParam(required = false) String repoUrl,
            @RequestParam(required = false) String liveUrl,
            Model model
    ) {
        try {
            projectAdminService.addManual(name, description, languages, repoUrl, liveUrl);
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("projectNameInput", name);
            model.addAttribute("projectDescriptionInput", description);
            model.addAttribute("projectLanguagesInput", languages);
            model.addAttribute("projectManualRepoUrlInput", repoUrl);
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
        model.addAttribute("projectError", error);
        return populatedDashboard(model);
    }

    private String dashboardWithCvError(Model model, String error) {
        model.addAttribute("cvError", error);
        return populatedDashboard(model);
    }

    /**
     * Replaces the downloadable CV. A rejected upload re-renders the dashboard
     * with the reason rather than redirecting, so the message survives.
     */
    @PostMapping("/cv")
    public String uploadCv(@RequestParam("cv") MultipartFile cv, Model model) {
        try {
            cvService.replace(cv);
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            return dashboardWithCvError(model, e.getMessage());
        } catch (IOException e) {
            return dashboardWithCvError(model, "Could not read that file. Try again.");
        }
    }

    @PostMapping("/cv/delete")
    public String deleteCv() {
        cvService.deleteCurrent();
        return "redirect:/admin";
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

    @PostMapping("/images/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String url = imageStorageService.upload(image);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Image storage is not configured."));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed."));
        }
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
