package com.kzzz3.argus.cortex.conversation.realtime;

import org.springframework.lang.Nullable;

public record ConversationRealtimeEvent(
        String conversationId,
        ConversationRealtimeEventType eventType,
        @Nullable String messageId,
        String occurredAt
) {
}
