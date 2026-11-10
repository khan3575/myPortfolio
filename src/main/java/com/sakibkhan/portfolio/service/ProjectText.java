package com.sakibkhan.portfolio.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a pasted-in "github-url: https://github.com/owner/repo.git" token out
 * of a project description. That token is a data field, not prose: left in the
 * description it is printed on the card as raw text, and the project never gets
 * a Source link. Every way a project enters the table runs its description
 * through {@link #split}, so the field ends up in the source URL instead.
 */
final class ProjectText {

    private static final Pattern GITHUB_URL_FIELD =
            Pattern.compile("\\s*\\bgithub-url:\\s*(https?://\\S+)", Pattern.CASE_INSENSITIVE);

    private ProjectText() {
    }

    /** The description with the field removed, and the URL it carried (null if there was none). */
    record Parts(String description, String sourceUrl) {}

    static Parts split(String rawDescription) {
        if (rawDescription == null) {
            return new Parts(null, null);
        }
        Matcher matcher = GITHUB_URL_FIELD.matcher(rawDescription);
        if (!matcher.find()) {
            String trimmed = rawDescription.trim();
            return new Parts(trimmed.isEmpty() ? null : trimmed, null);
        }
        String url = matcher.group(1).replaceAll("(?:\\.git)?/?$", "");
        String description = matcher.replaceAll("").trim();
        return new Parts(description.isEmpty() ? null : description, url);
    }
}
