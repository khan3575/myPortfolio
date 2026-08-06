package com.sakibkhan.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds src/main/resources/portfolio-data.yml. Read-only content: nothing
 * mutates this at runtime, which is why every nested shape is a record.
 * <p>
 * start/end fields are kept as plain "yyyy-MM" strings rather than
 * {@link java.time.YearMonth} — display formatting happens where it's
 * needed, not at bind time.
 */
@ConfigurationProperties(prefix = "portfolio")
public record PortfolioProperties(
        Hero hero,
        About about,
        Skills skills,
        List<Experience> experience,
        List<Education> education,
        List<Certification> certifications,
        List<Award> awards,
        Socials socials,
        List<ProjectFallback> projectsFallback
) {

    public record Hero(String name, String title, String status, List<Cta> ctas) {}

    public record Cta(String label, String href) {}

    public record About(String pitch, List<String> highlights) {}

    public record Skills(List<SkillCategory> categories) {}

    public record SkillCategory(String name, List<String> items) {}

    public record Experience(
            String role,
            String company,
            String location,
            String start,
            String end,
            List<String> bullets
    ) {}

    public record Education(
            String degree,
            String institution,
            String location,
            String start,
            String end,
            String detail
    ) {}

    public record Certification(String name, String issuer, String credentialId) {}

    public record Award(String name, String detail) {}

    public record Socials(String github, String linkedin, String email) {}

    /**
     * Used only when {@code GitHubProjectService} can't reach the GitHub API
     * (see design doc, Section 6) — the safety net, not the primary path.
     */
    public record ProjectFallback(String name, String description, String url, String language) {}
}
