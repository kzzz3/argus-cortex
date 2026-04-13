package com.kzzz3.argus.cortex.conversation.realtime;

public record ConversationRealtimeMessagePayload(
        String id,
        String conversationId,
        String senderDisplayName,
        String body,
        String timestampLabel,
        String deliveryStatus,
        String statusUpdatedAt
) {
}
