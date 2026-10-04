package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import com.sakibkhan.portfolio.model.Post;
import com.sakibkhan.portfolio.model.PostStatus;
import com.sakibkhan.portfolio.repository.PostRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * The two files a crawler asks for before it reads anything else. Generated
 * rather than static because the sitemap has to list blog posts, which live in
 * the database, and both need the site's public address.
 */
@RestController
public class SiteMetaController {

    private final PortfolioProperties portfolio;
    private final PostRepository postRepository;

    public SiteMetaController(PortfolioProperties portfolio, PostRepository postRepository) {
        this.portfolio = portfolio;
        this.postRepository = postRepository;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin

                Sitemap: %s/sitemap.xml
                """.formatted(portfolio.siteUrl());
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        appendUrl(xml, "/", null);
        appendUrl(xml, "/resume", null);
        appendUrl(xml, "/blog", null);
        for (Post post : postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED)) {
            String lastModified = post.getUpdatedAt() == null ? null : post.getUpdatedAt().toLocalDate().toString();
            appendUrl(xml, "/blog/" + post.getSlug(), lastModified);
        }
        return xml.append("</urlset>\n").toString();
    }

    private void appendUrl(StringBuilder xml, String path, String lastModified) {
        xml.append("  <url><loc>").append(HtmlUtils.htmlEscape(portfolio.siteUrl() + path)).append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>").append(lastModified).append("</lastmod>");
        }
        xml.append("</url>\n");
    }
}
