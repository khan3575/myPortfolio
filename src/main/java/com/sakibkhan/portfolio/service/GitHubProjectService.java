package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GitHubProjectService {

    private static final Logger log = LoggerFactory.getLogger(GitHubProjectService.class);
    private static final int MAX_PROJECTS = 6;

    private final RestClient restClient;
    private final PortfolioProperties portfolioProperties;
    private final String username;
    private final String token;

    public GitHubProjectService(
            RestClient.Builder restClientBuilder,
            PortfolioProperties portfolioProperties,
            @Value("${github.api.username}") String username,
            @Value("${github.api.token}") String token
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.github.com").build();
        this.portfolioProperties = portfolioProperties;
        this.username = username;
        this.token = token;
    }

    @Cacheable("githubProjects")
    public List<ProjectView> getProjects() {
        try {
            List<GitHubRepoResponse> repos = restClient.get()
                    .uri("/users/{username}/repos?sort=updated&direction=desc", username)
                    .headers(headers -> {
                        if (!token.isBlank()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<GitHubRepoResponse>>() {});

            if (repos == null) {
                return fallback();
            }

            return repos.stream()
                    .filter(repo -> !repo.fork())
                    .limit(MAX_PROJECTS)
                    .map(repo -> new ProjectView(repo.name(), repo.description(), repo.htmlUrl(), repo.language()))
                    .toList();
        } catch (Exception e) {
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
