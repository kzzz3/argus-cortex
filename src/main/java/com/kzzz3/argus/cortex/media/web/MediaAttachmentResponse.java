package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import java.time.LocalDateTime;

public record MediaAttachmentResponse(
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
    public static MediaAttachmentResponse from(MediaAttachmentRecord record) {
        return new MediaAttachmentResponse(
                record.attachmentId(),
                record.sessionId(),
                record.accountId(),
                record.conversationId(),
                record.attachmentType(),
                record.fileName(),
                record.contentType(),
                record.contentLength(),
                record.objectKey(),
                record.uploadUrl(),
                record.createdAt()
        );
    }
}
