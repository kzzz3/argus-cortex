package com.kzzz3.argus.cortex.conversation.realtime;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationRealtimeController {

    private final AccessTokenStore accessTokenStore;
    private final ConversationRealtimePublisher realtimePublisher;

    public ConversationRealtimeController(AccessTokenStore accessTokenStore, ConversationRealtimePublisher realtimePublisher) {
        this.accessTokenStore = accessTokenStore;
        this.realtimePublisher = realtimePublisher;
    }

    @GetMapping("/events")
    public SseEmitter streamEvents(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(extractBearerToken(authorizationHeader))
                .orElseThrow(InvalidCredentialsException::new);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        realtimePublisher.register(accountRecord.accountId(), emitter, lastEventId);
        emitter.onCompletion(() -> realtimePublisher.unregister(accountRecord.accountId(), emitter));
        emitter.onTimeout(() -> {
            realtimePublisher.unregister(accountRecord.accountId(), emitter);
            emitter.complete();
        });
        emitter.onError(error -> {
            realtimePublisher.unregister(accountRecord.accountId(), emitter);
            emitter.completeWithError(error);
        });
        return emitter;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new IllegalArgumentException("Missing Authorization header.");
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new IllegalArgumentException("Authorization header must use Bearer token.");
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }
}
