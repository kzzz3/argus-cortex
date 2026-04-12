package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.application.MediaUploadSessionApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/upload-sessions")
public class MediaUploadSessionController {

    private final MediaUploadSessionApplicationService mediaUploadSessionApplicationService;

    public MediaUploadSessionController(MediaUploadSessionApplicationService mediaUploadSessionApplicationService) {
        this.mediaUploadSessionApplicationService = mediaUploadSessionApplicationService;
    }

    @PostMapping
    public MediaUploadSessionResponse createUploadSession(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateMediaUploadSessionRequest request
    ) {
        return MediaUploadSessionResponse.from(mediaUploadSessionApplicationService.createUploadSession(
                extractBearerToken(authorizationHeader),
                request
        ));
    }

    @PostMapping("/{sessionId}/finalize")
    public MediaAttachmentResponse finalizeUploadSession(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String sessionId,
            @Valid @RequestBody FinalizeMediaUploadRequest request
    ) {
        return MediaAttachmentResponse.from(mediaUploadSessionApplicationService.finalizeUploadSession(
                extractBearerToken(authorizationHeader),
                sessionId,
                request
        ));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new IllegalArgumentException("Missing Authorization header.");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new IllegalArgumentException("Authorization header must use Bearer token.");
        }

        return authorizationHeader.substring(prefix.length()).trim();
    }
}
