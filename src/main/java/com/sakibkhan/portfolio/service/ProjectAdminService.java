package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.Project;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProjectAdminService {

    private static final Logger log = LoggerFactory.getLogger(ProjectAdminService.class);

    // Owner/repo segments, an optional trailing ".git" and/or slash -- covers
    // what browsers actually put in the address bar and what "git clone"
    // URLs look like, without trying to be a general URL parser.
    private static final Pattern REPO_URL_PATTERN =
            Pattern.compile("^https?://(?:www\\.)?github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?/?$");
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://.+");

    private final GitHubApiClient apiClient;
    private final ProjectRepository projectRepository;

    public ProjectAdminService(GitHubApiClient apiClient, ProjectRepository projectRepository) {
        this.apiClient = apiClient;
        this.projectRepository = projectRepository;
    }

    /**
     * Looks the repo up on GitHub so name/description/language reflect the
     * real repo rather than whatever was typed, then stores it. The
     * duplicate check runs against GitHub's own {@code html_url} (not the
     * raw pasted string) so two URLs that differ only in case or a trailing
     * slash are still recognized as the same repo.
     */
    public Project addFromGitHub(String rawRepoUrl, String rawLiveUrl) {
        String trimmed = rawRepoUrl == null ? "" : rawRepoUrl.trim();
        Matcher matcher = REPO_URL_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Enter a GitHub repo URL, like https://github.com/owner/repo.");
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        GitHubRepoResponse repoData;
        try {
            repoData = apiClient.fetchRepo(owner, repo);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Couldn't find that repo on GitHub — check the URL and that it's public.");
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            // Unauthenticated GitHub API calls are capped at 60/hour per IP; a
            // GITHUB_TOKEN env var raises that to 5,000/hour. This is by far
            // the most common failure here, so it gets its own message rather
            // than falling into the generic one below.
            log.warn("GitHub API rate limit hit while adding a project: {}", e.getMessage());
            throw new IllegalArgumentException(
                    "GitHub API rate limit exceeded. Set a GITHUB_TOKEN env var for a much higher limit, or try again later.");
        } catch (Exception e) {
            log.warn("GitHub API call failed while adding a project", e);
            throw new IllegalArgumentException("GitHub API error, try again in a moment.");
        }

        if (projectRepository.existsBySourceUrl(repoData.htmlUrl())) {
            throw new IllegalArgumentException("That repo is already on the list.");
        }

        Project project = Project.builder()
                .name(repoData.name())
                .description(repoData.description())
                .sourceUrl(repoData.htmlUrl())
                .liveUrl(normalizeUrl(rawLiveUrl))
                .language(repoData.language())
                .build();

        return projectRepository.save(project);
    }

    /**
     * For a project with no public repo to point to -- closed-source, or not
     * pushed anywhere public yet. Backed only by a live/demo link and
     * hand-typed details, since there's no GitHub API call to pull them from.
     */
    public Project addManual(String name, String description, String language, String rawLiveUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        String liveUrl = normalizeUrl(rawLiveUrl);
        if (liveUrl == null) {
            throw new IllegalArgumentException("A live URL is required for a project with no GitHub repo.");
        }

        Project project = Project.builder()
                .name(name.trim())
                .description(blankToNull(description))
                .language(blankToNull(language))
                .liveUrl(liveUrl)
                .build();

        return projectRepository.save(project);
    }

    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        projectRepository.deleteById(id);
    }

    private String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!HTTP_URL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Live URL must start with http:// or https://");
        }
        return trimmed;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
