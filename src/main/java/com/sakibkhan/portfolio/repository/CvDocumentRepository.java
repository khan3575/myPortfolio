package com.sakibkhan.portfolio.repository;

import com.sakibkhan.portfolio.model.CvDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CvDocumentRepository extends JpaRepository<CvDocument, Long> {

    /**
     * Everything about the current CV except the bytes. The homepage and the
     * résumé page only need to know whether one exists and how big it is;
     * loading the PDF itself to render a link would be pure waste.
     */
    interface Summary {
        String getFilename();

        long getSizeBytes();

        LocalDateTime getUploadedAt();
    }

    /** The bytes, for the two download endpoints. */
    Optional<CvDocument> findFirstByOrderByUploadedAtDescIdDesc();

    /** Same row, metadata only. */
    Optional<Summary> findFirstSummaryByOrderByUploadedAtDescIdDesc();
}
