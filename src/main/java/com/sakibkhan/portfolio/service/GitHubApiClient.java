package com.sakibkhan.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to the GitHub API to look up a single repo's public metadata --
 * used when an admin pastes a repo URL into the Projects panel, so its
 * name/description/language don't have to be typed in by hand.
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
                .headers(headers -> {
                    if (!token.isBlank()) {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve()
                .body(GitHubRepoResponse.class);

        if (response == null) {
            throw new IllegalStateException("GitHub API returned no data for " + owner + "/" + repo);
        }
        return response;
    }
}
