package com.kzzz3.argus.cortex.media.domain;

import java.time.LocalDateTime;

public record MediaAttachmentRecord(
        String attachmentId,
        String sessionId,
        String accountId,
        String conversationId,
        MediaAttachmentType attachmentType,
        String fileName,
        String contentType,
        long contentLength,
        String objectKey,
        String uploadUrl,
        LocalDateTime createdAt
) {
}
