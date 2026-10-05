package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.config.PortfolioProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The homepage's content as a schema.org Person, for the JSON-LD block in its
 * head. A crawler or profile parser reading the visible page has to infer
 * which line is the job title and which is the university; this states it.
 * Built from the same {@link PortfolioProperties} the page renders, so the two
 * cannot drift apart.
 */
@Component
public class PersonStructuredData {

    private final PortfolioProperties portfolio;

    public PersonStructuredData(PortfolioProperties portfolio) {
        this.portfolio = portfolio;
    }

    /** Insertion-ordered so the rendered JSON reads top-down the way the page does. */
    public Map<String, Object> build() {
        Map<String, Object> person = new LinkedHashMap<>();
        person.put("@context", "https://schema.org");
        person.put("@type", "Person");
        person.put("name", portfolio.hero().name());
        person.put("jobTitle", portfolio.hero().title());
        person.put("description", portfolio.description());
        person.put("url", portfolio.siteUrl());
        person.put("email", portfolio.socials().email());

        Map<String, Object> address = address(portfolio.socials().location());
        if (address != null) {
            person.put("address", address);
        }

        List<String> profiles = new ArrayList<>();
        profiles.add(portfolio.socials().github());
        profiles.add(portfolio.socials().linkedin());
        if (portfolio.socials().medium() != null) {
            profiles.add(portfolio.socials().medium());
        }
        portfolio.codingProfiles().forEach(profile -> profiles.add(profile.url()));
        person.put("sameAs", profiles);

        person.put("knowsAbout", portfolio.skills().categories().stream()
                .flatMap(category -> category.items().stream())
                .toList());

        person.put("alumniOf", portfolio.education().stream()
                .map(edu -> typed("CollegeOrUniversity", edu.institution()))
                .toList());

        List<Map<String, Object>> credentials = new ArrayList<>();
        for (PortfolioProperties.Education edu : portfolio.education()) {
            Map<String, Object> degree = typed("EducationalOccupationalCredential", edu.degree());
            degree.put("credentialCategory", "degree");
            degree.put("recognizedBy", typed("CollegeOrUniversity", edu.institution()));
            credentials.add(degree);
        }
        for (PortfolioProperties.Certification cert : portfolio.certifications()) {
            Map<String, Object> certificate = typed("EducationalOccupationalCredential", cert.name());
            certificate.put("credentialCategory", "certificate");
            certificate.put("recognizedBy", typed("Organization", cert.issuer()));
            if (cert.credentialUrl() != null) {
                certificate.put("url", cert.credentialUrl());
            }
            credentials.add(certificate);
        }
        person.put("hasCredential", credentials);

        person.put("award", portfolio.awards().stream()
                .map(award -> award.name() + " — " + award.detail())
                .toList());

        return person;
    }

    private static Map<String, Object> typed(String type, String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("@type", type);
        node.put("name", name);
        return node;
    }

    /** "Dhaka, Bangladesh" -> locality and country; anything else is kept whole as the locality. */
    private static Map<String, Object> address(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        String[] parts = location.split(",");
        if (parts.length == 2) {
            address.put("addressLocality", parts[0].trim());
            address.put("addressCountry", parts[1].trim());
        } else {
            address.put("addressLocality", location.trim());
        }
        return address;
    }
}
