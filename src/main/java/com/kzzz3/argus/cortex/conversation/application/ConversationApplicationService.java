package com.kzzz3.argus.cortex.conversation.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeEvent;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeEventType;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeMessagePayload;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimePublisher;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationApplicationService {

    private static final int DEFAULT_RECENT_WINDOW_DAYS = 7;
	private static final int DEFAULT_MESSAGE_LIMIT = 50;

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
    private final ConversationStore conversationStore;
    private final ConversationRealtimePublisher conversationRealtimePublisher;
    private final MediaAttachmentStore mediaAttachmentStore;

	public ConversationApplicationService(
			AuthenticatedAccountResolver authenticatedAccountResolver,
			ConversationStore conversationStore,
			ConversationRealtimePublisher conversationRealtimePublisher,
			MediaAttachmentStore mediaAttachmentStore
	) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
        this.conversationStore = conversationStore;
        this.conversationRealtimePublisher = conversationRealtimePublisher;
        this.mediaAttachmentStore = mediaAttachmentStore;
    }

	public List<ConversationSummary> listConversations(int recentWindowDays) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
        return conversationStore.listConversations(accountRecord, normalizedWindowDays);
    }

	public ConversationDetail getConversationDetail(String conversationId) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        return conversationStore.getConversationDetail(accountRecord, conversationId);
    }

	public ConversationMessagePage listMessages(
			String conversationId,
			int recentWindowDays,
			int limit,
			String sinceCursor
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
        int normalizedLimit = normalizeMessageLimit(limit);
        return conversationStore.listMessages(accountRecord, conversationId, normalizedWindowDays, normalizedLimit, sinceCursor);
    }

	@Transactional
	public ConversationMessage sendMessage(
			String conversationId,
			SendMessageCommand request
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
		ConversationMessageAttachment attachment = resolveAttachment(accountRecord, conversationId, request.attachmentId());
        String normalizedBody = normalizeMessageBody(request.body(), attachment);
        ConversationMessage message = conversationStore.sendMessage(
                accountRecord,
                conversationId,
                request.clientMessageId().trim(),
                normalizedBody,
                attachment
        );
        if (!message.duplicateClientMessage()) {
            publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_CREATED, message);
        }
        return message;
    }

	@Transactional
	public ConversationMessage applyReceipt(
			String conversationId,
			String messageId,
			ApplyMessageReceiptCommand request
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        ConversationMessage message = conversationStore.applyReceipt(accountRecord, conversationId, messageId, request.receiptType().trim());
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_STATUS_UPDATED, message);
        return message;
    }

	@Transactional
	public ConversationMessage recallMessage(
			String conversationId,
			String messageId
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        ConversationMessage message = conversationStore.recallMessage(accountRecord, conversationId, messageId);
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.MESSAGE_RECALLED, message);
        return message;
    }

	@Transactional
	public ConversationSummary markConversationRead(
			String conversationId
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        ConversationSummary summary = conversationStore.markConversationRead(accountRecord, conversationId);
        publishRealtimeEvent(conversationId, ConversationRealtimeEventType.CONVERSATION_READ, null);
        return summary;
    }

    private void publishRealtimeEvent(String conversationId, ConversationRealtimeEventType eventType, ConversationMessage message) {
        conversationRealtimePublisher.publish(new ConversationRealtimeEvent(
                "",
                conversationId,
                eventType,
                message == null ? null : message.id(),
                message == null ? null : toRealtimePayload(message),
                OffsetDateTime.now().toString(),
                false
        ));
    }

    private ConversationRealtimeMessagePayload toRealtimePayload(ConversationMessage message) {
        return new ConversationRealtimeMessagePayload(
                message.id(),
                message.conversationId(),
                message.senderDisplayName(),
                message.body(),
                message.timestampLabel(),
                message.deliveryStatus(),
                message.statusUpdatedAt(),
                message.attachment()
        );
    }

    @Nullable
	private ConversationMessageAttachment resolveAttachment(
			AccountRecord accountRecord,
			String conversationId,
			@Nullable String attachmentId
	) {
		if (attachmentId == null || attachmentId.isBlank()) {
			return null;
		}
		MediaAttachmentRecord attachmentRecord = mediaAttachmentStore.findByAttachmentId(attachmentId.trim())
				.orElseThrow(() -> new IllegalArgumentException("Attachment not found."));
        if (!accountRecord.accountId().equals(attachmentRecord.accountId())) {
            throw new IllegalArgumentException("Attachment does not belong to the authenticated account.");
        }
        if (attachmentRecord.conversationId() != null && !attachmentRecord.conversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Attachment does not belong to the requested conversation.");
        }
        return new ConversationMessageAttachment(
                attachmentRecord.attachmentId(),
                attachmentRecord.attachmentType().name(),
                attachmentRecord.fileName(),
                attachmentRecord.contentType(),
                attachmentRecord.contentLength()
        );
    }

    private String normalizeMessageBody(@Nullable String rawBody, @Nullable ConversationMessageAttachment attachment) {
        String normalizedBody = rawBody == null ? "" : rawBody.trim();
        if (!normalizedBody.isEmpty()) {
            return normalizedBody;
        }
        if (attachment != null) {
            return attachment.fileName();
        }
        throw new IllegalArgumentException("Message body or attachment is required.");
    }

    private int normalizeRecentWindowDays(int requestedWindowDays) {
        return requestedWindowDays <= 0 ? DEFAULT_RECENT_WINDOW_DAYS : Math.min(requestedWindowDays, 30);
    }

    private int normalizeMessageLimit(int requestedLimit) {
        return requestedLimit <= 0 ? DEFAULT_MESSAGE_LIMIT : Math.min(requestedLimit, 200);
    }
}
