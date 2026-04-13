package com.kzzz3.argus.cortex.conversation.realtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ConversationRealtimeEventBuffer {

    private static final int MAX_EVENTS_PER_ACCOUNT = 200;

    private final AtomicLong nextEventId = new AtomicLong();
    private final ConcurrentMap<String, CopyOnWriteArrayList<ConversationRealtimeEvent>> eventsByAccount = new ConcurrentHashMap<>();

    public String allocateEventId() {
        return Long.toString(nextEventId.incrementAndGet());
    }

    public void append(String accountId, ConversationRealtimeEvent event) {
        CopyOnWriteArrayList<ConversationRealtimeEvent> queue = eventsByAccount.computeIfAbsent(
                accountId,
                key -> new CopyOnWriteArrayList<>()
        );
        queue.add(event);
        while (queue.size() > MAX_EVENTS_PER_ACCOUNT) {
            queue.remove(0);
        }
    }

    public List<ConversationRealtimeEvent> eventsAfter(String accountId, String lastEventId) {
        CopyOnWriteArrayList<ConversationRealtimeEvent> events = eventsByAccount.get(accountId);
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        long cursor = parseEventId(lastEventId);
        List<ConversationRealtimeEvent> replayEvents = new ArrayList<>();
        for (ConversationRealtimeEvent event : events) {
            if (parseEventId(event.eventId()) > cursor) {
                replayEvents.add(event.asReplayed());
            }
        }
        return replayEvents;
    }

    private long parseEventId(String rawEventId) {
        if (rawEventId == null || rawEventId.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(rawEventId.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
