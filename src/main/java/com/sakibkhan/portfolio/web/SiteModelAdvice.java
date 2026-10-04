package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Puts the two values every page's head needs into every model, so the blog
 * and résumé controllers don't each have to remember to add them before the
 * shared {@code seo} fragment can build a canonical link.
 */
@ControllerAdvice
public class SiteModelAdvice {

    private final PortfolioProperties portfolio;

    public SiteModelAdvice(PortfolioProperties portfolio) {
        this.portfolio = portfolio;
    }

    @ModelAttribute("siteUrl")
    public String siteUrl() {
        return portfolio.siteUrl();
    }

    @ModelAttribute("siteDescription")
    public String siteDescription() {
        return portfolio.description();
    }
}
