package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GitHubProjectService {

    private static final Logger log = LoggerFactory.getLogger(GitHubProjectService.class);

    private final GitHubApiClient apiClient;
    private final PortfolioProperties portfolioProperties;

    public GitHubProjectService(GitHubApiClient apiClient, PortfolioProperties portfolioProperties) {
        this.apiClient = apiClient;
        this.portfolioProperties = portfolioProperties;
    }

    public List<ProjectView> getProjects() {
        try {
            return apiClient.fetchLiveProjects();
        } catch (Exception e) {
            // Falling back here, outside the @Cacheable method, means this
            // failure is never cached -- the next request tries GitHub again
            // instead of being stuck on the static list for the full TTL.
            log.warn("GitHub API unavailable, falling back to configured project list: {}", e.getMessage());
            return fallback();
        }
    }

    private List<ProjectView> fallback() {
        return portfolioProperties.projectsFallback().stream()
                .map(p -> new ProjectView(p.name(), p.description(), p.url(), p.language()))
                .toList();
    }
}
