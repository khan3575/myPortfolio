package com.sakibkhan.portfolio.service;

/** Unified shape for a showcased project, regardless of whether it came from the GitHub API or the config fallback. */
public record ProjectView(String name, String description, String url, String language) {}
