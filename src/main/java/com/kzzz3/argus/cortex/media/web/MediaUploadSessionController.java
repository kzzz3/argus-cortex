package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.application.MediaUploadSessionApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/{sessionId}/content")
    public void uploadContent(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        String accessToken = extractBearerToken(authorizationHeader);
        try {
            byte[] content = request.getInputStream().readAllBytes();
            mediaUploadSessionApplicationService.uploadContent(accessToken, sessionId, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read upload payload.", ex);
        }
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
