package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import java.util.Map;

public record MediaUploadSessionResponse(
        String sessionId,
        MediaAttachmentType attachmentType,
        String uploadUrl,
        long maxPayloadBytes,
        String uploadToken,
        Map<String, String> uploadHeaders,
        String instructions
) {
    public static MediaUploadSessionResponse from(MediaUploadSession session) {
        return new MediaUploadSessionResponse(
                session.sessionId(),
                session.attachmentType(),
                session.uploadUrl(),
                session.maxPayloadBytes(),
                session.uploadToken(),
                Map.copyOf(session.uploadHeaders()),
                session.instructions()
        );
    }
}
