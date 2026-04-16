package com.kzzz3.argus.cortex.conversation.realtime;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationRealtimeController {

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final ConversationRealtimePublisher realtimePublisher;

	public ConversationRealtimeController(AuthenticatedAccountResolver authenticatedAccountResolver, ConversationRealtimePublisher realtimePublisher) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
		this.realtimePublisher = realtimePublisher;
	}

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        realtimePublisher.register(accountRecord.accountId(), emitter, lastEventId);
        emitter.onCompletion(() -> realtimePublisher.unregister(accountRecord.accountId(), emitter));
        emitter.onTimeout(() -> {
            realtimePublisher.unregister(accountRecord.accountId(), emitter);
            emitter.complete();
        });
        emitter.onError(error -> {
            realtimePublisher.unregister(accountRecord.accountId(), emitter);
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // Response already closed by container/client disconnect.
            }
        });
        return emitter;
    }

}
