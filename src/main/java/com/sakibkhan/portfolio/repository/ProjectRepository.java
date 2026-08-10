package com.sakibkhan.portfolio.repository;

import com.sakibkhan.portfolio.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Newest-added first, for both the homepage and the admin list. */
    List<Project> findAllByOrderByCreatedAtDesc();

    boolean existsBySourceUrl(String sourceUrl);
}
