package com.sakibkhan.portfolio.web;

import com.sakibkhan.portfolio.model.CvDocument;
import com.sakibkhan.portfolio.service.CvService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * The two public CV routes. /cv opens in the browser's PDF viewer -- the
 * shareable link -- while /cv/download is the one a recruiter clicks to get
 * the file onto disk. Both are unauthenticated: that is the whole point.
 */
@Controller
public class CvController {

    private final CvService cvService;

    public CvController(CvService cvService) {
        this.cvService = cvService;
    }

    @GetMapping("/cv")
    public ResponseEntity<byte[]> view() {
        return respond("inline");
    }

    @GetMapping("/cv/download")
    public ResponseEntity<byte[]> download() {
        return respond("attachment");
    }

    private ResponseEntity<byte[]> respond(String disposition) {
        CvDocument cv = cvService.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No CV uploaded yet"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder(disposition)
                .filename(cv.getFilename(), StandardCharsets.UTF_8)
                .build());
        // Re-uploads reuse this URL, so the copy a visitor caches has to be
        // revalidated rather than served for a week out of a proxy.
        headers.setCacheControl("no-cache, must-revalidate");

        return ResponseEntity.ok().headers(headers).body(cv.getData());
    }
}
