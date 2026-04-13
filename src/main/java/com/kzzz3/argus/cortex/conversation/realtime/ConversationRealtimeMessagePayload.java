package com.kzzz3.argus.cortex.conversation.realtime;

import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import org.springframework.lang.Nullable;

public record ConversationRealtimeMessagePayload(
        String id,
        String conversationId,
        String senderDisplayName,
        String body,
        String timestampLabel,
        String deliveryStatus,
        String statusUpdatedAt,
        @Nullable ConversationMessageAttachment attachment
) {
}
