package com.kzzz3.argus.cortex.conversation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimeEvent;
import com.kzzz3.argus.cortex.conversation.realtime.ConversationRealtimePublisher;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationApplicationServiceTest {

	@Test
	void retryingSameClientMessageIdDoesNotPublishDuplicateRealtimeEvent() {
		AccountRecord account = new AccountRecord("alice", "Alice", "hash");
		DeduplicatingConversationStore conversationStore = new DeduplicatingConversationStore();
		CapturingRealtimePublisher realtimePublisher = new CapturingRealtimePublisher();
		ConversationApplicationService service = new ConversationApplicationService(
				new FixedAuthenticatedAccountResolver(account),
				conversationStore,
				realtimePublisher,
				new EmptyMediaAttachmentStore()
		);

		ConversationMessage first = service.sendMessage("direct-alice-bob", new SendMessageCommand("client-1", "hello", null));
		ConversationMessage retried = service.sendMessage("direct-alice-bob", new SendMessageCommand("client-1", "hello", null));

		assertEquals(first.id(), retried.id());
		assertFalse(first.duplicateClientMessage());
		assertTrue(retried.duplicateClientMessage());
		assertEquals(1, realtimePublisher.events.size());
	}

	private static final class FixedAuthenticatedAccountResolver extends AuthenticatedAccountResolver {
		private final AccountRecord account;

		private FixedAuthenticatedAccountResolver(AccountRecord account) {
			super(new RejectingJwtDecoder(), new EmptyAccountStore());
			this.account = account;
		}

		@Override
		public AccountRecord resolveCurrent() {
			return account;
		}
	}

	private static final class DeduplicatingConversationStore implements ConversationStore {
		private final List<ConversationMessage> messages = new ArrayList<>();

		@Override
		public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
			return List.of();
		}

		@Override
		public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
			return new ConversationDetail(conversationId, "Direct", "", 1, List.of(accountRecord.displayName()));
		}

		@Override
		public ConversationMessagePage listMessages(AccountRecord accountRecord, String conversationId, int recentWindowDays, int limit, @Nullable String sinceCursor) {
			return new ConversationMessagePage(messages, "", recentWindowDays, limit);
		}

		@Override
		public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body, @Nullable ConversationMessageAttachment attachment) {
			Optional<ConversationMessage> existing = messages.stream()
					.filter(message -> message.id().equals(clientMessageId))
					.findFirst();
			if (existing.isPresent()) {
				ConversationMessage message = existing.get();
				return new ConversationMessage(
						message.id(),
						message.conversationId(),
						message.senderDisplayName(),
						message.body(),
						message.timestampLabel(),
						message.fromCurrentUser(),
						message.deliveryStatus(),
						message.statusUpdatedAt(),
						message.attachment(),
						true
				);
			}
			else {
						ConversationMessage message = new ConversationMessage(
								clientMessageId,
								conversationId,
								accountRecord.displayName(),
								body,
								"now",
								true,
								"SENT",
								OffsetDateTime.now().toString(),
								attachment
						);
						messages.add(message);
						return message;
			}
		}

		@Override
		public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
			return messages.getFirst();
		}

		@Override
		public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
			return messages.getFirst();
		}

		@Override
		public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
			return new ConversationSummary(conversationId, "Direct", "", "", "", 0, "");
		}

		@Override
		public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
			return new ConversationSummary("direct", friend.displayName(), "", "", "", 0, "");
		}
	}

	private static final class CapturingRealtimePublisher implements ConversationRealtimePublisher {
		private final List<ConversationRealtimeEvent> events = new ArrayList<>();

		@Override
		public void register(String accountId, SseEmitter emitter, String lastEventId) {
		}

		@Override
		public void unregister(String accountId, SseEmitter emitter) {
		}

		@Override
		public void publish(ConversationRealtimeEvent event) {
			events.add(event);
		}
	}

	private static final class EmptyMediaAttachmentStore implements MediaAttachmentStore {
		@Override
		public MediaAttachmentRecord save(MediaAttachmentRecord record) {
			return record;
		}

		@Override
		public Optional<MediaAttachmentRecord> findByAttachmentId(String attachmentId) {
			return Optional.empty();
		}
	}

	private static final class RejectingJwtDecoder implements JwtDecoder {
		@Override
		public org.springframework.security.oauth2.jwt.Jwt decode(String token) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class EmptyAccountStore implements AccountStore {
		@Override
		public boolean exists(String accountId) {
			return false;
		}

		@Override
		public AccountRecord save(AccountRecord accountRecord) {
			return accountRecord;
		}

		@Override
		public Optional<AccountRecord> findByAccountId(String accountId) {
			return Optional.empty();
		}
	}
}
