package com.sakibkhan.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to the GitHub API, cached on success only. Kept as a separate bean
 * (rather than a private method on {@link GitHubProjectService}) so the
 * {@code @Cacheable} advice actually applies -- Spring's AOP proxy can't
 * intercept a method calling itself internally. Throwing here is what makes
 * a transient failure NOT get cached; {@code @Cacheable} only stores a
 * normal return value, never a thrown exception.
 */
@Component
class GitHubApiClient {

    private static final int MAX_PROJECTS = 6;

    private final RestClient restClient;
    private final String username;
    private final String token;

    GitHubApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${github.api.username}") String username,
            @Value("${github.api.token}") String token
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.github.com").build();
        this.username = username;
        this.token = token;
    }

    @Cacheable("githubProjects")
    public List<ProjectView> fetchLiveProjects() {
        List<GitHubRepoResponse> repos = restClient.get()
                .uri("/users/{username}/repos?sort=updated&direction=desc", username)
                .headers(headers -> {
                    if (!token.isBlank()) {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {});

        if (repos == null) {
            throw new IllegalStateException("GitHub API returned no data");
        }

        return repos.stream()
                .filter(repo -> !repo.fork())
                .limit(MAX_PROJECTS)
                .map(repo -> new ProjectView(repo.name(), repo.description(), repo.htmlUrl(), repo.language()))
                .toList();
    }
}
