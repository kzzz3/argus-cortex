package com.kzzz3.argus.cortex.conversation.realtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ConversationRealtimePublisher {

    void register(String accountId, SseEmitter emitter, String lastEventId);

    void unregister(String accountId, SseEmitter emitter);

    void publish(ConversationRealtimeEvent event);
}
