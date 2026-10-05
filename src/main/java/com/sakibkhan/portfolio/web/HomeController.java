package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import com.sakibkhan.portfolio.service.CvService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final int BLOG_PREVIEW_COUNT = 3;

    private final PortfolioProperties portfolioProperties;
    private final ProjectRepository projectRepository;
    private final BlogEntries blogEntries;
    private final CvService cvService;
    private final PersonStructuredData personStructuredData;

    public HomeController(
            PortfolioProperties portfolioProperties,
            ProjectRepository projectRepository,
            BlogEntries blogEntries,
            CvService cvService,
            PersonStructuredData personStructuredData
    ) {
        this.portfolioProperties = portfolioProperties;
        this.projectRepository = projectRepository;
        this.blogEntries = blogEntries;
        this.cvService = cvService;
        this.personStructuredData = personStructuredData;
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

        model.addAttribute("recentPosts", blogEntries.latest(BLOG_PREVIEW_COUNT));
        model.addAttribute("cv", cvService.currentSummary().orElse(null));
        model.addAttribute("personJsonLd", personStructuredData.build());

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
