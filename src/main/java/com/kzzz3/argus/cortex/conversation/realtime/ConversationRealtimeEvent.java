package com.kzzz3.argus.cortex.conversation.realtime;

import org.springframework.lang.Nullable;

public record ConversationRealtimeEvent(
        String eventId,
        String conversationId,
        ConversationRealtimeEventType eventType,
        @Nullable String messageId,
        @Nullable ConversationRealtimeMessagePayload message,
        String occurredAt,
        boolean replayed
) {
    public ConversationRealtimeEvent withEventId(String nextEventId) {
        return new ConversationRealtimeEvent(nextEventId, conversationId, eventType, messageId, message, occurredAt, replayed);
    }

    public ConversationRealtimeEvent asReplayed() {
        return new ConversationRealtimeEvent(eventId, conversationId, eventType, messageId, message, occurredAt, true);
    }
}
