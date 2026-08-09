package com.sakibkhan.portfolio.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    // commonmark passes raw HTML embedded in the source straight through
    // (e.g. a pasted <script> tag), so the rendered output is untrusted
    // until it's been through this policy -- render() must never return
    // commonmark's output directly.
    private static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.TABLES);

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        String rawHtml = renderer.render(document);
        return SANITIZER_POLICY.sanitize(rawHtml);
    }
}
