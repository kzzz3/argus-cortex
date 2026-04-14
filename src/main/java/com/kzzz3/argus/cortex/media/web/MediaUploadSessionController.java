package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.application.MediaUploadSessionApplicationService;
import com.kzzz3.argus.cortex.media.application.CreateMediaUploadSessionCommand;
import com.kzzz3.argus.cortex.media.application.FinalizeMediaUploadCommand;
import com.kzzz3.argus.cortex.media.application.MediaUploadSessionApplicationService.MediaAttachmentDownload;
import com.kzzz3.argus.cortex.shared.web.BearerTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaUploadSessionController {

    private final MediaUploadSessionApplicationService mediaUploadSessionApplicationService;

    public MediaUploadSessionController(MediaUploadSessionApplicationService mediaUploadSessionApplicationService) {
        this.mediaUploadSessionApplicationService = mediaUploadSessionApplicationService;
    }

    @PostMapping("/upload-sessions")
	public MediaUploadSessionResponse createUploadSession(
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody CreateMediaUploadSessionRequest request
	) {
		return MediaUploadSessionResponse.from(mediaUploadSessionApplicationService.createUploadSession(
				BearerTokenExtractor.extract(authorizationHeader),
				new CreateMediaUploadSessionCommand(request.attachmentType(), request.fileName(), request.estimatedBytes())
		));
	}

    @PostMapping("/upload-sessions/{sessionId}/finalize")
	public MediaAttachmentResponse finalizeUploadSession(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String sessionId,
            @Valid @RequestBody FinalizeMediaUploadRequest request
    ) {
		return MediaAttachmentResponse.from(mediaUploadSessionApplicationService.finalizeUploadSession(
				BearerTokenExtractor.extract(authorizationHeader),
				sessionId,
				new FinalizeMediaUploadCommand(
						request.fileName(),
						request.contentType(),
						request.contentLength(),
						request.objectKey(),
						request.conversationId()
				)
		));
	}

    @PutMapping("/upload-sessions/{sessionId}/content")
    public void uploadContent(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String sessionId,
            HttpServletRequest request
	) {
		String accessToken = BearerTokenExtractor.extract(authorizationHeader);
        try {
            byte[] content = request.getInputStream().readAllBytes();
            mediaUploadSessionApplicationService.uploadContent(accessToken, sessionId, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read upload payload.", ex);
        }
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String attachmentId
	) {
		String accessToken = BearerTokenExtractor.extract(authorizationHeader);
		MediaAttachmentDownload download = mediaUploadSessionApplicationService.loadAttachment(accessToken, attachmentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String candidateContentType = download.record().contentType();
        if (candidateContentType != null && !candidateContentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(candidateContentType);
            } catch (InvalidMediaTypeException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.record().fileName(), StandardCharsets.UTF_8)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(disposition);
        headers.setContentLength(download.content().length);
        return new ResponseEntity<>(download.content(), headers, HttpStatus.OK);
    }

}
