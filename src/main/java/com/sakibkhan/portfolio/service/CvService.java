package com.sakibkhan.portfolio.service;

import com.sakibkhan.portfolio.model.CvDocument;
import com.sakibkhan.portfolio.repository.CvDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * Stores the single current CV. Uploads are restricted to PDFs -- that is what
 * a recruiter expects to open, and it keeps the download endpoint from ever
 * serving something a browser would render as HTML in the site's own origin.
 */
@Service
public class CvService {

    /** Comfortably above a text-based CV, low enough that a stray upload can't fill the row. */
    static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final String FALLBACK_FILENAME = "cv.pdf";

    private final CvDocumentRepository cvDocumentRepository;

    public CvService(CvDocumentRepository cvDocumentRepository) {
        this.cvDocumentRepository = cvDocumentRepository;
    }

    /** The current CV with its bytes, or empty if none has been uploaded yet. */
    @Transactional(readOnly = true)
    public Optional<CvDocument> current() {
        return cvDocumentRepository.findFirstByOrderByUploadedAtDescIdDesc();
    }

    /** The current CV's metadata only -- what the pages need to decide whether to link to it. */
    @Transactional(readOnly = true)
    public Optional<CvDocumentRepository.Summary> currentSummary() {
        return cvDocumentRepository.findFirstSummaryByOrderByUploadedAtDescIdDesc();
    }

    /**
     * Replaces the stored CV. The old row goes first so that /cv keeps pointing
     * at exactly one file rather than accumulating superseded copies.
     *
     * @throws IllegalArgumentException with a message meant for the admin screen
     */
    @Transactional
    public CvDocument replace(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a PDF to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("That file is larger than " + (MAX_BYTES / 1024 / 1024) + "MB.");
        }

        byte[] bytes = file.getBytes();
        if (!looksLikePdf(bytes)) {
            throw new IllegalArgumentException("That doesn't look like a PDF.");
        }

        cvDocumentRepository.deleteAll();
        return cvDocumentRepository.save(CvDocument.builder()
                .filename(sanitizeFilename(file.getOriginalFilename()))
                .contentType("application/pdf")
                .sizeBytes(bytes.length)
                .data(bytes)
                .build());
    }

    @Transactional
    public void deleteCurrent() {
        cvDocumentRepository.deleteAll();
    }

    /**
     * Trusts the magic bytes rather than the browser-supplied content type,
     * which is trivially spoofed and, for an admin-only upload, occasionally
     * just wrong.
     */
    private boolean looksLikePdf(byte[] bytes) {
        return bytes.length > PDF_MAGIC.length
                && Arrays.equals(bytes, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length);
    }

    /**
     * Strips any directory part the browser sent and reduces the rest to
     * characters that are safe in a Content-Disposition filename.
     */
    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return FALLBACK_FILENAME;
        }
        String name = Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            return FALLBACK_FILENAME;
        }
        return name.toLowerCase().endsWith(".pdf") ? name : name + ".pdf";
    }
}
