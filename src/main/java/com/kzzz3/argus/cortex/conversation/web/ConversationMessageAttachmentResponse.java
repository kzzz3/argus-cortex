package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;

public record ConversationMessageAttachmentResponse(
        String attachmentId,
        String attachmentType,
        String fileName,
        String contentType,
        long contentLength
) {
    public static ConversationMessageAttachmentResponse from(ConversationMessageAttachment attachment) {
        return new ConversationMessageAttachmentResponse(
                attachment.attachmentId(),
                attachment.attachmentType(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.contentLength()
        );
    }
}
