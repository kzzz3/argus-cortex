package com.kzzz3.argus.cortex.conversation.domain;

public record ConversationMessageAttachment(
        String attachmentId,
        String attachmentType,
        String fileName,
        String contentType,
        long contentLength
) {
}
