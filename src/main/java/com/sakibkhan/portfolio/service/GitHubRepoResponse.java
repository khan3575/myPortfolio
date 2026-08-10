package com.sakibkhan.portfolio.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoResponse(
        String name,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        String language
) {}
