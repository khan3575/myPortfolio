package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.Project;
import com.sakibkhan.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectAdminServiceTest {

    private static final String LEAKED =
            "Asset tracking system with a database-enforced audit trail  github-url: https://github.com/khan3575/Asset-Tagging-System.git";

    @Test
    void splitMovesTheGithubUrlFieldOutOfTheDescription() {
        ProjectText.Parts parts = ProjectText.split(LEAKED);

        assertThat(parts.description()).isEqualTo("Asset tracking system with a database-enforced audit trail");
        assertThat(parts.sourceUrl()).isEqualTo("https://github.com/khan3575/Asset-Tagging-System");
    }

    @Test
    void splitLeavesPlainDescriptionsAlone() {
        assertThat(ProjectText.split("  Just prose.  ")).isEqualTo(new ProjectText.Parts("Just prose.", null));
        assertThat(ProjectText.split(null)).isEqualTo(new ProjectText.Parts(null, null));
    }

    @Test
    void manualProjectNeverKeepsTheFieldInItsDescription() {
        ProjectRepository repository = mock(ProjectRepository.class);
        when(repository.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));
        ProjectAdminService service = new ProjectAdminService(mock(GitHubApiClient.class), repository);

        Project saved = service.addManual("Asset Tagging System", LEAKED, "Java",
                null, "https://assets.sakib-khan.com");

        assertThat(saved.getDescription()).doesNotContain("github-url");
        assertThat(saved.getSourceUrl()).isEqualTo("https://github.com/khan3575/Asset-Tagging-System");
        assertThat(saved.getLiveUrl()).isEqualTo("https://assets.sakib-khan.com");
    }

    @Test
    void githubProjectNeverKeepsTheFieldInItsDescription() {
        GitHubApiClient api = mock(GitHubApiClient.class);
        when(api.fetchRepo("khan3575", "Asset-Tagging-System")).thenReturn(new GitHubRepoResponse(
                "Asset-Tagging-System", LEAKED, "https://github.com/khan3575/Asset-Tagging-System", "Java"));
        ProjectRepository repository = mock(ProjectRepository.class);
        when(repository.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));
        ProjectAdminService service = new ProjectAdminService(api, repository);

        Project saved = service.addFromGitHub("https://github.com/khan3575/Asset-Tagging-System", null);

        assertThat(saved.getDescription()).doesNotContain("github-url");
    }

    @Test
    void typedTagsGetTheirProperSpelling() {
        ProjectRepository repository = mock(ProjectRepository.class);
        when(repository.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));
        ProjectAdminService service = new ProjectAdminService(mock(GitHubApiClient.class), repository);

        Project saved = service.addManual("X", null, "Java, Mysql, spring boot, mysql",
                null, "https://example.com");

        assertThat(saved.getLanguages()).containsExactly("Java", "MySQL", "Spring Boot");
    }
}
