package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import com.sakibkhan.portfolio.service.CvService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final int BLOG_PREVIEW_COUNT = 3;

    private final PortfolioProperties portfolioProperties;
    private final ProjectRepository projectRepository;
    private final PostRepository postRepository;
    private final CvService cvService;

    public HomeController(
            PortfolioProperties portfolioProperties,
            ProjectRepository projectRepository,
            PostRepository postRepository,
            CvService cvService
    ) {
        this.portfolioProperties = portfolioProperties;
        this.projectRepository = projectRepository;
        this.postRepository = postRepository;
        this.cvService = cvService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("hero", portfolioProperties.hero());
        model.addAttribute("about", portfolioProperties.about());
        model.addAttribute("skills", portfolioProperties.skills());
        model.addAttribute("experience", portfolioProperties.experience());
        model.addAttribute("education", portfolioProperties.education());
        model.addAttribute("certifications", portfolioProperties.certifications());
        model.addAttribute("organizations", portfolioProperties.organizations());
        model.addAttribute("awards", portfolioProperties.awards());
        model.addAttribute("codingProfiles", portfolioProperties.codingProfiles());
        model.addAttribute("socials", portfolioProperties.socials());
        model.addAttribute("projects", projectRepository.findAllByOrderByCreatedAtDesc());

        var recentPosts = postRepository.findByStatusOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PageRequest.of(0, BLOG_PREVIEW_COUNT));
        model.addAttribute("recentPosts", recentPosts);
        model.addAttribute("cv", cvService.currentSummary().orElse(null));

        return "index";
    }

    @GetMapping("/resume")
    public String resume(Model model) {
        model.addAttribute("hero", portfolioProperties.hero());
        model.addAttribute("socials", portfolioProperties.socials());
        model.addAttribute("cv", cvService.currentSummary().orElse(null));
        return "resume";
    }
}
