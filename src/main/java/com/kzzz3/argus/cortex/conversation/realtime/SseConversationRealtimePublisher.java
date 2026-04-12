package com.kzzz3.argus.cortex.conversation.realtime;

import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationThreadMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseConversationRealtimePublisher implements ConversationRealtimePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseConversationRealtimePublisher.class);

    private final ConversationThreadMapper threadMapper;
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emittersByAccount = new ConcurrentHashMap<>();

    public SseConversationRealtimePublisher(ConversationThreadMapper threadMapper) {
        this.threadMapper = threadMapper;
    }

    @Override
    public void register(String accountId, SseEmitter emitter) {
        emittersByAccount.computeIfAbsent(accountId, key -> new CopyOnWriteArrayList<>()).add(emitter);
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
        List<String> accountIds;
        try {
            accountIds = threadMapper.selectOwnerAccountIdsByConversationId(event.conversationId());
        } catch (RuntimeException ex) {
            LOGGER.debug("Unable to resolve realtime recipients for {}: {}", event.conversationId(), ex.getMessage());
            return;
        }
        if (accountIds.isEmpty()) {
            return;
        }
        for (String accountId : accountIds) {
            sendEventToAccount(event, accountId);
        }
    }

    private void sendEventToAccount(ConversationRealtimeEvent event, String accountId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByAccount.get(accountId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.occurredAt())
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
}
