package com.kzzz3.argus.cortex.conversation.realtime;

import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationThreadMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseConversationRealtimePublisher implements ConversationRealtimePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseConversationRealtimePublisher.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    private final ConversationThreadMapper threadMapper;
    private final ConversationRealtimeEventBuffer eventBuffer;
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emittersByAccount = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "conversation-realtime-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public SseConversationRealtimePublisher(
            ConversationThreadMapper threadMapper,
            ConversationRealtimeEventBuffer eventBuffer
    ) {
        this.threadMapper = threadMapper;
        this.eventBuffer = eventBuffer;
        heartbeatExecutor.scheduleAtFixedRate(this::publishHeartbeats, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void register(String accountId, SseEmitter emitter, String lastEventId) {
        emittersByAccount.computeIfAbsent(accountId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        replayMissedEvents(accountId, emitter, lastEventId);
        sendControlEvent(accountId, emitter, ConversationRealtimeEventType.STREAM_READY);
    }

    @Override
    public void unregister(String accountId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByAccount.get(accountId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByAccount.remove(accountId, emitters);
        }
    }

    @Override
    public void publish(ConversationRealtimeEvent event) {
        ConversationRealtimeEvent resolvedEvent = ensureEventId(event);
        List<String> accountIds;
        try {
            accountIds = threadMapper.selectOwnerAccountIdsByConversationId(resolvedEvent.conversationId());
        } catch (RuntimeException ex) {
            LOGGER.debug("Unable to resolve realtime recipients for {}: {}", resolvedEvent.conversationId(), ex.getMessage());
            return;
        }
        if (accountIds.isEmpty()) {
            return;
        }
        for (String accountId : accountIds) {
            eventBuffer.append(accountId, resolvedEvent);
            sendEventToAccount(resolvedEvent, accountId);
        }
    }

    @PreDestroy
    void stopHeartbeatExecutor() {
        heartbeatExecutor.shutdownNow();
    }

    private void replayMissedEvents(String accountId, SseEmitter emitter, String lastEventId) {
        for (ConversationRealtimeEvent replayEvent : eventBuffer.eventsAfter(accountId, lastEventId)) {
            sendEvent(emitter, replayEvent, accountId);
        }
    }

    private void publishHeartbeats() {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emittersByAccount.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                sendControlEvent(entry.getKey(), emitter, ConversationRealtimeEventType.HEARTBEAT);
            }
        }
    }

    private void sendControlEvent(String accountId, SseEmitter emitter, ConversationRealtimeEventType eventType) {
        ConversationRealtimeEvent event = new ConversationRealtimeEvent(
                eventBuffer.allocateEventId(),
                "",
                eventType,
                null,
                null,
                OffsetDateTime.now().toString(),
                false
        );
        sendEvent(emitter, event, accountId);
    }

    private ConversationRealtimeEvent ensureEventId(ConversationRealtimeEvent event) {
        if (event.eventId() != null && !event.eventId().isBlank()) {
            return event;
        }
        return event.withEventId(eventBuffer.allocateEventId());
    }

    private void sendEventToAccount(ConversationRealtimeEvent event, String accountId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByAccount.get(accountId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, event, accountId);
        }
    }

    private void sendEvent(SseEmitter emitter, ConversationRealtimeEvent event, String accountId) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.eventId())
                    .name("conversation-event")
                    .data(event));
        } catch (IOException | IllegalStateException ex) {
            LOGGER.warn("Failed to send SSE event to account {}: {}", accountId, ex.getMessage());
            unregister(accountId, emitter);
            try {
                emitter.completeWithError(ex);
            } catch (IllegalStateException ignored) {
                emitter.complete();
            }
        }
    }
}
