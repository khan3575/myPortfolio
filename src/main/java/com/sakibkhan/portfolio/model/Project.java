package com.sakibkhan.portfolio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /** Null for a project with no public repo to link to. */
    @Column(name = "source_url", unique = true, length = 500)
    private String sourceUrl;

    /** The deployed/demo link -- what makes a closed-source project showable at all. */
    @Column(name = "live_url", length = 500)
    private String liveUrl;

    @Column(length = 100)
    private String language;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** What a visitor should actually click: the live site if there is one, else the source. */
    public String getPrimaryUrl() {
        return liveUrl != null ? liveUrl : sourceUrl;
    }
}
