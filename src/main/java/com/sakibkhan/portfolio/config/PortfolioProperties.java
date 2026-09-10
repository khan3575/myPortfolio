package com.sakibkhan.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Binds src/main/resources/portfolio-data.yml. Read-only content: nothing
 * mutates this at runtime, which is why every nested shape is a record.
 * <p>
 * start/end fields are kept as plain "yyyy-MM" strings rather than
 * {@link java.time.YearMonth} — they are easier to read and edit that way
 * in the data file. Rendering goes through {@code dateRange()} rather than the
 * raw accessors, so a page never prints "2026-03" at a visitor.
 */
@ConfigurationProperties(prefix = "portfolio")
public record PortfolioProperties(
        Hero hero,
        About about,
        Skills skills,
        List<Experience> experience,
        List<Education> education,
        List<Certification> certifications,
        List<Organization> organizations,
        List<Award> awards,
        List<CodingProfile> codingProfiles,
        Socials socials
) {

    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /**
     * "2026-03" -> "March 2026". Anything unparseable is handed back untouched:
     * a typo in the data file should surface as an odd-looking date on the page,
     * not as a 500 on the homepage.
     */
    private static String formatMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(yearMonth.trim()).format(MONTH_YEAR);
        } catch (DateTimeParseException e) {
            return yearMonth;
        }
    }

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
    ) {
        /** "March 2026 – September 2026", or "… – Present" for the job still held. */
        public String dateRange() {
            return formatMonth(start) + " – " + (end == null ? "Present" : formatMonth(end));
        }
    }

    public record Education(
            String degree,
            String institution,
            String location,
            String start,
            String end,
            String detail
    ) {
        /** "January 2022 – April 2026". */
        public String dateRange() {
            return formatMonth(start) + " – " + (end == null ? "Present" : formatMonth(end));
        }
    }

    public record Certification(String name, String issuer, String credentialId, String credentialUrl) {}

    /** Communities and clubs held a role in -- the "& Organizations" half of the CV's certificates section. */
    public record Organization(String role, String name, String period) {}

    public record Award(String name, String detail) {}

    public record CodingProfile(String platform, String url) {}

    public record Socials(String github, String linkedin, String email, String secondaryEmail, String location) {}
}
