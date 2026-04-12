package com.kzzz3.argus.cortex.media.domain;

import java.util.Map;

public record MediaUploadSession(
        String sessionId,
        String accountId,
        MediaAttachmentType attachmentType,
        String objectKey,
        String uploadUrl,
        long maxPayloadBytes,
        String uploadToken,
        Map<String, String> uploadHeaders,
        String instructions,
        boolean uploaded
) {
}
