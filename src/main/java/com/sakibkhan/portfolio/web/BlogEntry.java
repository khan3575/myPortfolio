package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.ExternalPost;
import com.sakibkhan.portfolio.model.ExternalPostSource;
import com.sakibkhan.portfolio.model.Post;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * One card on the blog, whichever kind of post is behind it. The templates
 * render a single list of these, so an article on Medium and one written
 * here sort together by date and differ only in where the link leads.
 *
 * @param siteName where an external article lives ("Medium"); null for an on-site post
 */
public record BlogEntry(
        String title,
        String summary,
        String imageUrl,
        LocalDateTime publishedAt,
        String href,
        String siteName
) {

    public boolean external() {
        return siteName != null;
    }

    static BlogEntry of(Post post) {
        return new BlogEntry(post.getTitle(), post.getSummary(), null, post.getPublishedAt(),
                "/blog/" + post.getSlug(), null);
    }

    static BlogEntry of(ExternalPost post) {
        return new BlogEntry(post.getTitle(), post.getSummary(), post.getImageUrl(), post.getPublishedAt(),
                post.getUrl(), siteName(post));
    }

    /** "Medium" for a feed import; a hand-added link is labelled with its host. */
    private static String siteName(ExternalPost post) {
        if (post.getSource() == ExternalPostSource.MEDIUM) {
            return "Medium";
        }
        try {
            String host = URI.create(post.getUrl()).getHost();
            return host == null ? "another site" : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return "another site";
        }
    }
}
