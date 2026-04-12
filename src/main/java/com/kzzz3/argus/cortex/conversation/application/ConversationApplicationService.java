package com.kzzz3.argus.cortex.conversation.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeEvent;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeEventType;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimePublisher;
import com.kzzz3.argus.cortex.conversation.web.CreateConversationRequest;
import com.kzzz3.argus.cortex.conversation.web.AddConversationMemberRequest;
import com.kzzz3.argus.cortex.conversation.web.MessageReceiptRequest;
import com.kzzz3.argus.cortex.conversation.web.SendMessageRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationApplicationService {

    private static final int DEFAULT_RECENT_WINDOW_DAYS = 7;
    private static final int DEFAULT_MESSAGE_LIMIT = 50;

    private final AccessTokenStore accessTokenStore;
    private final AccountStore accountStore;
    private final ConversationStore conversationStore;
    private final ConversationRealtimePublisher conversationRealtimePublisher;

    public ConversationApplicationService(
            AccessTokenStore accessTokenStore,
            AccountStore accountStore,
            ConversationStore conversationStore,
            ConversationRealtimePublisher conversationRealtimePublisher
    ) {
        this.accessTokenStore = accessTokenStore;
        this.accountStore = accountStore;
        this.conversationStore = conversationStore;
        this.conversationRealtimePublisher = conversationRealtimePublisher;
    }

    public List<ConversationSummary> listConversations(String accessToken, int recentWindowDays) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
        return conversationStore.listConversations(accountRecord, normalizedWindowDays);
    }

    public ConversationDetail getConversationDetail(String accessToken, String conversationId) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        return conversationStore.getConversationDetail(accountRecord, conversationId);
    }

    public ConversationMessagePage listMessages(
            String accessToken,
            String conversationId,
            int recentWindowDays,
            int limit,
            String sinceCursor
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
        int normalizedLimit = normalizeMessageLimit(limit);
        return conversationStore.listMessages(accountRecord, conversationId, normalizedWindowDays, normalizedLimit, sinceCursor);
    }

    public ConversationMessage sendMessage(
            String accessToken,
            String conversationId,
            SendMessageRequest request
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        ConversationMessage message = conversationStore.sendMessage(
                accountRecord,
                conversationId,
                request.clientMessageId().trim(),
                request.body().trim()
        );
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_CREATED, message.id());
        return message;
    }

    public ConversationMessage applyReceipt(
            String accessToken,
            String conversationId,
            String messageId,
            MessageReceiptRequest request
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        ConversationMessage message = conversationStore.applyReceipt(accountRecord, conversationId, messageId, request.receiptType().trim());
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_STATUS_UPDATED, message.id());
        return message;
    }

    public ConversationMessage recallMessage(
            String accessToken,
            String conversationId,
            String messageId
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        ConversationMessage message = conversationStore.recallMessage(accountRecord, conversationId, messageId);
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_RECALLED, message.id());
        return message;
    }

    public ConversationSummary markConversationRead(
            String accessToken,
            String conversationId
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        ConversationSummary summary = conversationStore.markConversationRead(accountRecord, conversationId);
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.CONVERSATION_READ, null);
        return summary;
    }

    public ConversationSummary createConversation(
            String accessToken,
            CreateConversationRequest request
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        ConversationSummary summary = conversationStore.createConversation(accountRecord, request.type().trim(), request.title().trim());
        publishRealtimeEvent(summary.id(), ConversationRealtimeEventType.CONVERSATION_CREATED, null);
        return summary;
    }

    public ConversationDetail addMember(
            String accessToken,
            String conversationId,
            AddConversationMemberRequest request
    ) {
        AccountRecord owner = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        AccountRecord member = accountStore.findByAccountId(request.memberAccountId().trim())
                .orElseThrow(InvalidCredentialsException::new);
        ConversationDetail detail = conversationStore.addMember(owner, conversationId, member);
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.CONVERSATION_UPDATED, null);
        return detail;
    }

    private void publishRealtimeEvent(String conversationId, ConversationRealtimeEventType eventType, String messageId) {
        conversationRealtimePublisher.publish(new ConversationRealtimeEvent(conversationId, eventType, messageId, OffsetDateTime.now().toString()));
    }

    private int normalizeRecentWindowDays(int requestedWindowDays) {
        return requestedWindowDays <= 0 ? DEFAULT_RECENT_WINDOW_DAYS : Math.min(requestedWindowDays, 30);
    }

    private int normalizeMessageLimit(int requestedLimit) {
        return requestedLimit <= 0 ? DEFAULT_MESSAGE_LIMIT : Math.min(requestedLimit, 200);
    }
}
