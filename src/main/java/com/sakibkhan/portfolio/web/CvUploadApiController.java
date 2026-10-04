package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.CvDocument;
import com.sakibkhan.portfolio.service.CvService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Lets the CV repository's build pipeline replace the CV without a browser.
 * The admin form at /admin/cv needs a login session and a CSRF token, neither
 * of which a CI job has; this route takes a shared bearer token instead and
 * hands the file to the same {@link CvService}, so the PDF checks are
 * identical whichever way a file arrives.
 */
@RestController
public class CvUploadApiController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CvService cvService;
    private final byte[] uploadToken;

    public CvUploadApiController(CvService cvService, @Value("${cv.upload-token}") String uploadToken) {
        this.cvService = cvService;
        this.uploadToken = uploadToken.strip().getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping(value = "/api/cv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "cv", required = false) MultipartFile cv
    ) {
        // No token configured means the route is switched off, not open: an
        // unset variable must never turn into an upload anyone can make.
        if (uploadToken.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found\n");
        }
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or wrong upload token\n");
        }

        try {
            CvDocument stored = cvService.replace(cv);
            return ResponseEntity.ok("Stored " + stored.getFilename() + " (" + stored.getSizeBytes() + " bytes)\n");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage() + "\n");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not read that file\n");
        }
    }

    /** Compared in constant time so the response time says nothing about how much of a guess matched. */
    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] presented = authorization.substring(BEARER_PREFIX.length()).strip().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(presented, uploadToken);
    }
}
