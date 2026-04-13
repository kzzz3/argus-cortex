package com.kzzz3.argus.cortex.conversation.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationRealtimeEventBufferTest {

    @Test
    void eventsAfterReturnsOnlyMissedEventsAsReplay() {
        ConversationRealtimeEventBuffer buffer = new ConversationRealtimeEventBuffer();
        ConversationRealtimeEvent first = new ConversationRealtimeEvent(
                buffer.allocateEventId(),
                "conv-1",
                ConversationRealtimeEventType.MESSAGE_CREATED,
                "msg-1",
                null,
                "2026-04-13T10:00:00Z",
                false
        );
        ConversationRealtimeEvent second = new ConversationRealtimeEvent(
                buffer.allocateEventId(),
                "conv-1",
                ConversationRealtimeEventType.MESSAGE_STATUS_UPDATED,
                "msg-1",
                null,
                "2026-04-13T10:00:01Z",
                false
        );
        buffer.append("tester", first);
        buffer.append("tester", second);

        List<ConversationRealtimeEvent> replayEvents = buffer.eventsAfter("tester", first.eventId());

        assertEquals(1, replayEvents.size());
        assertEquals(second.eventId(), replayEvents.get(0).eventId());
        assertTrue(replayEvents.get(0).replayed());
    }

    @Test
    void appendKeepsOnlyMostRecentBoundedEvents() {
        ConversationRealtimeEventBuffer buffer = new ConversationRealtimeEventBuffer();
        for (int index = 0; index < 205; index++) {
            ConversationRealtimeEvent event = new ConversationRealtimeEvent(
                    buffer.allocateEventId(),
                    "conv-1",
                    ConversationRealtimeEventType.MESSAGE_CREATED,
                    "msg-" + index,
                    null,
                    "2026-04-13T10:00:00Z",
                    false
            );
            buffer.append("tester", event);
        }

        List<ConversationRealtimeEvent> replayEvents = buffer.eventsAfter("tester", "0");

        assertEquals(200, replayEvents.size());
        assertEquals("6", replayEvents.get(0).eventId());
        assertEquals("205", replayEvents.get(replayEvents.size() - 1).eventId());
    }
}
