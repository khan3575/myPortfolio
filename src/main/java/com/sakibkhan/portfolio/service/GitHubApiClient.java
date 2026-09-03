package com.sakibkhan.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Talks to the GitHub API to look up a single repo's public metadata --
 * used when an admin pastes a repo URL into the Projects panel, so its
 * name/description/languages don't have to be typed in by hand.
 */
@Component
class GitHubApiClient {

    private final RestClient restClient;
    private final String token;

    GitHubApiClient(RestClient.Builder restClientBuilder, @Value("${github.api.token}") String token) {
        this.restClient = restClientBuilder.baseUrl("https://api.github.com").build();
        this.token = token;
    }

    public GitHubRepoResponse fetchRepo(String owner, String repo) {
        GitHubRepoResponse response = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(this::authorize)
                .retrieve()
                .body(GitHubRepoResponse.class);

        if (response == null) {
            throw new IllegalStateException("GitHub API returned no data for " + owner + "/" + repo);
        }
        return response;
    }

    /**
     * Every language GitHub detected in the repo, biggest share first. The
     * repo endpoint only reports the single primary language, which
     * undersells anything carrying a real front end next to its backend.
     */
    public List<String> fetchLanguages(String owner, String repo) {
        Map<String, Long> byteCounts = restClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .headers(this::authorize)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Long>>() {});

        if (byteCounts == null) {
            return List.of();
        }
        return byteCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void authorize(HttpHeaders headers) {
        if (!token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }
}
