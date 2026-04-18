package com.kzzz3.argus.cortex.conversation.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryConversationStore implements ConversationStore {

	private final Map<String, LinkedHashMap<String, AccountConversationState>> conversationsByAccount = new ConcurrentHashMap<>();
	private final Map<String, SharedConversationState> sharedConversationsById = new ConcurrentHashMap<>();

	@Override
	public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
		return getOrCreateConversations(accountRecord.accountId()).values().stream()
				.map(threadState -> threadState.toSummary(accountRecord.accountId(), sharedConversationsById.get(threadState.id())))
				.toList();
	}

	@Override
	public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
		AccountConversationState threadState = requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		return new ConversationDetail(
				threadState.id(),
				threadState.title(),
				threadState.subtitle(),
				sharedConversationState.memberDisplayNames().size(),
				new ArrayList<>(sharedConversationState.memberDisplayNames().values())
		);
	}

	@Override
	public ConversationMessagePage listMessages(AccountRecord accountRecord, String conversationId, int recentWindowDays, int limit, String sinceCursor) {
		AccountConversationState threadState = requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		if (threadState.cursor().equals(sinceCursor)) {
			return new ConversationMessagePage(List.of(), threadState.cursor(), recentWindowDays, limit);
		}

		ConversationSyncCursor currentCursor = ConversationSyncCursor.parse(conversationId, threadState.cursor());
		ConversationSyncCursor requestedCursor = ConversationSyncCursor.parse(conversationId, sinceCursor);
		if (requestedCursor != null && currentCursor != null) {
			if (currentCursor.sequence() == requestedCursor.sequence() && currentCursor.revision() > requestedCursor.revision()) {
				return new ConversationMessagePage(List.of(), threadState.cursor(), recentWindowDays, limit);
			}
			if (currentCursor.sequence() > requestedCursor.sequence()) {
				List<ConversationMessage> continuationMessages = sharedConversationState.messagesAfter(accountRecord.accountId(), requestedCursor.sequence(), limit);
				String nextCursor = continuationMessages.isEmpty()
						? threadState.cursor()
						: ConversationSyncCursor.of(conversationId, requestedCursor.sequence() + continuationMessages.size(), currentCursor.revision()).encoded();
				return new ConversationMessagePage(continuationMessages, nextCursor, recentWindowDays, limit);
			}
		}

		return new ConversationMessagePage(
				sharedConversationState.recentMessages(accountRecord.accountId(), limit),
				threadState.cursor(),
				recentWindowDays,
				limit
		);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body, @Nullable ConversationMessageAttachment attachment) {
		AccountConversationState threadState = requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		String normalizedClientMessageId = clientMessageId == null || clientMessageId.isBlank() ? null : clientMessageId.trim();
		StoredConversationMessage existing = sharedConversationState.findByClientMessageId(accountRecord.accountId(), normalizedClientMessageId);
		if (existing != null) {
			return existing.toConversationMessage(accountRecord.accountId());
		}

		StoredConversationMessage message = sharedConversationState.addMessage(
				accountRecord.accountId(),
				accountRecord.displayName(),
				normalizedClientMessageId,
				body.trim(),
				attachment
		);
		advanceAllConversationViews(sharedConversationState, message.sequenceNo(), accountRecord.accountId());
		return message.toConversationMessage(accountRecord.accountId());
	}

	@Override
	public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
		requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		StoredConversationMessage message = sharedConversationState.requireMessage(messageId);
		if (accountRecord.accountId().equals(message.senderAccountId())) {
			return message.toConversationMessage(accountRecord.accountId());
		}
		if (message.applyReceipt(receiptType)) {
			advanceAllConversationViews(sharedConversationState, message.sequenceNo(), null);
		}
		return message.toConversationMessage(accountRecord.accountId());
	}

	@Override
	public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
		requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		StoredConversationMessage message = sharedConversationState.requireMessage(messageId);
		if (!accountRecord.accountId().equals(message.senderAccountId())) {
			throw new IllegalArgumentException("Only the sender can recall this message.");
		}
		message.recall();
		advanceAllConversationViews(sharedConversationState, message.sequenceNo(), null);
		return message.toConversationMessage(accountRecord.accountId());
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		AccountConversationState threadState = requireConversation(accountRecord.accountId(), conversationId);
		SharedConversationState sharedConversationState = requireSharedConversation(conversationId);
		threadState.markRead(sharedConversationState.latestSequence());
		return threadState.toSummary(accountRecord.accountId(), sharedConversationState);
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		SharedConversationState sharedConversationState = sharedConversationsById.computeIfAbsent(
				conversationId,
				ignored -> new SharedConversationState(conversationId)
		);
		sharedConversationState.ensureMember(owner.accountId(), owner.displayName());
		sharedConversationState.ensureMember(friend.accountId(), friend.displayName());
		AccountConversationState ownerThread = getOrCreateConversations(owner.accountId())
				.computeIfAbsent(conversationId, ignored -> new AccountConversationState(
						conversationId,
						friend.displayName(),
						"Direct friend conversation"
				));
			getOrCreateConversations(friend.accountId())
					.computeIfAbsent(conversationId, ignored -> new AccountConversationState(
							conversationId,
							owner.displayName(),
							"Direct friend conversation"
					));
			return ownerThread.toSummary(owner.accountId(), sharedConversationState);
	}

	private AccountConversationState requireConversation(String accountId, String conversationId) {
		AccountConversationState threadState = getOrCreateConversations(accountId).get(conversationId);
		if (threadState == null) {
			throw new ConversationNotFoundException(conversationId);
		}
		return threadState;
	}

	private SharedConversationState requireSharedConversation(String conversationId) {
		SharedConversationState sharedConversationState = sharedConversationsById.get(conversationId);
		if (sharedConversationState == null) {
			throw new ConversationNotFoundException(conversationId);
		}
		return sharedConversationState;
	}

	private LinkedHashMap<String, AccountConversationState> getOrCreateConversations(String accountId) {
		return conversationsByAccount.computeIfAbsent(accountId, ignored -> new LinkedHashMap<>());
	}

	private void advanceAllConversationViews(SharedConversationState sharedConversationState, long affectedSequenceNo, @Nullable String senderAccountId) {
		for (String participantAccountId : sharedConversationState.memberDisplayNames().keySet()) {
			AccountConversationState threadState = requireConversation(participantAccountId, sharedConversationState.id());
			if (senderAccountId != null && !senderAccountId.equals(participantAccountId)) {
				threadState.incrementUnread(affectedSequenceNo);
			} else {
				threadState.touch(affectedSequenceNo);
			}
		}
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}

	private static final class AccountConversationState {
		private final String id;
		private final String title;
		private final String subtitle;
		private int unreadCount;
		private long latestMessageSequence;
		private long syncRevision;

		private AccountConversationState(String id, String title, String subtitle) {
			this.id = id;
			this.title = title;
			this.subtitle = subtitle;
		}

		private String id() {
			return id;
		}

		private String title() {
			return title;
		}

		private String subtitle() {
			return subtitle;
		}

		private void touch(long affectedSequenceNo) {
			latestMessageSequence = Math.max(latestMessageSequence, affectedSequenceNo);
			syncRevision += 1;
		}

		private void incrementUnread(long affectedSequenceNo) {
			unreadCount += 1;
			touch(affectedSequenceNo);
		}

		private void markRead(long latestSequence) {
			unreadCount = 0;
			touch(latestSequence);
		}

		private String cursor() {
			return ConversationSyncCursor.of(id, latestMessageSequence, syncRevision).encoded();
		}

		private ConversationSummary toSummary(String viewerAccountId, SharedConversationState sharedConversationState) {
			StoredConversationMessage latestMessage = sharedConversationState.latestMessage();
			return new ConversationSummary(
					id,
					title,
					subtitle,
					latestMessage == null ? "No messages yet" : latestMessage.renderBody(viewerAccountId),
					latestMessage == null ? "--:--" : latestMessage.timestampLabel(),
					unreadCount,
					cursor()
			);
		}
	}

	private static final class SharedConversationState {
		private final String id;
		private final LinkedHashMap<String, String> memberDisplayNames = new LinkedHashMap<>();
		private final List<StoredConversationMessage> messages = new ArrayList<>();
		private long latestSequence;

		private SharedConversationState(String id) {
			this.id = id;
		}

		private String id() {
			return id;
		}

		private LinkedHashMap<String, String> memberDisplayNames() {
			return memberDisplayNames;
		}

		private void ensureMember(String accountId, String displayName) {
			memberDisplayNames.putIfAbsent(accountId, displayName);
		}

		private long latestSequence() {
			return latestSequence;
		}

		@Nullable
		private StoredConversationMessage latestMessage() {
			return messages.isEmpty() ? null : messages.get(messages.size() - 1);
		}

		@Nullable
		private StoredConversationMessage findByClientMessageId(String senderAccountId, @Nullable String clientMessageId) {
			if (clientMessageId == null || clientMessageId.isBlank()) {
				return null;
			}
			return messages.stream()
					.filter(message -> senderAccountId.equals(message.senderAccountId()) && clientMessageId.equals(message.clientMessageId()))
					.findFirst()
					.orElse(null);
		}

		private StoredConversationMessage addMessage(
				String senderAccountId,
				String senderDisplayName,
				@Nullable String clientMessageId,
				String body,
				@Nullable ConversationMessageAttachment attachment
		) {
			latestSequence += 1;
			StoredConversationMessage message = new StoredConversationMessage(
					"msg-" + UUID.randomUUID(),
					clientMessageId == null ? "msg-client-" + latestSequence : clientMessageId,
					id,
					senderAccountId,
					senderDisplayName,
					body,
					attachment,
					latestSequence
			);
			messages.add(message);
			return message;
		}

		private StoredConversationMessage requireMessage(String messageId) {
			return messages.stream()
					.filter(message -> message.id().equals(messageId))
					.findFirst()
					.orElseThrow(() -> new MessageNotFoundException(messageId));
		}

		private List<ConversationMessage> recentMessages(String viewerAccountId, int limit) {
			int fromIndex = Math.max(messages.size() - limit, 0);
			return messages.subList(fromIndex, messages.size()).stream()
					.map(message -> message.toConversationMessage(viewerAccountId))
					.toList();
		}

		private List<ConversationMessage> messagesAfter(String viewerAccountId, long sequenceNo, int limit) {
			int fromIndex = Math.min(Math.max((int) sequenceNo, 0), messages.size());
			int toIndex = Math.min(fromIndex + limit, messages.size());
			return messages.subList(fromIndex, toIndex).stream()
					.map(message -> message.toConversationMessage(viewerAccountId))
					.toList();
		}
	}

	private static final class StoredConversationMessage {
		private final String id;
		private final String clientMessageId;
		private final String conversationId;
		private final String senderAccountId;
		private final String senderDisplayName;
		private final long sequenceNo;
		private final LocalDateTime createdAt;
		private final String timestampLabel;
		private String body;
		@Nullable
		private final ConversationMessageAttachment attachment;
		private String deliveryStatus;
		private String statusUpdatedAt;

		private StoredConversationMessage(
				String id,
				String clientMessageId,
				String conversationId,
				String senderAccountId,
				String senderDisplayName,
				String body,
				@Nullable ConversationMessageAttachment attachment,
				long sequenceNo
		) {
			this.id = id;
			this.clientMessageId = clientMessageId;
			this.conversationId = conversationId;
			this.senderAccountId = senderAccountId;
			this.senderDisplayName = senderDisplayName;
			this.body = body;
			this.attachment = attachment;
			this.sequenceNo = sequenceNo;
			this.createdAt = ConversationTimeFormatter.nowLocal();
			this.timestampLabel = ConversationTimeFormatter.formatTimestampLabel(createdAt);
			this.deliveryStatus = "SENT";
			this.statusUpdatedAt = ConversationTimeFormatter.formatStatusUpdatedAt(ConversationTimeFormatter.nowOffset());
		}

		private String id() {
			return id;
		}

		private String clientMessageId() {
			return clientMessageId;
		}

		private String senderAccountId() {
			return senderAccountId;
		}

		private long sequenceNo() {
			return sequenceNo;
		}

		private String timestampLabel() {
			return timestampLabel;
		}

		private boolean applyReceipt(@Nullable String receiptType) {
			if ("RECALLED".equals(deliveryStatus)) {
				return false;
			}
			String nextStatus = normalizeReceiptType(receiptType);
			if (deliveryRank(nextStatus) <= deliveryRank(deliveryStatus)) {
				return false;
			}
			deliveryStatus = nextStatus;
			statusUpdatedAt = ConversationTimeFormatter.formatStatusUpdatedAt(ConversationTimeFormatter.nowOffset());
			return true;
		}

		private void recall() {
			body = "Message recalled";
			deliveryStatus = "RECALLED";
			statusUpdatedAt = ConversationTimeFormatter.formatStatusUpdatedAt(ConversationTimeFormatter.nowOffset());
		}

		private String renderBody(String viewerAccountId) {
			if ("RECALLED".equals(deliveryStatus)) {
				return viewerAccountId.equals(senderAccountId)
						? "You recalled a message"
						: "Message recalled";
			}
			return body;
		}

		private ConversationMessage toConversationMessage(String viewerAccountId) {
			return new ConversationMessage(
					id,
					conversationId,
					senderDisplayName,
					renderBody(viewerAccountId),
					timestampLabel,
					viewerAccountId.equals(senderAccountId),
					deliveryStatus,
					statusUpdatedAt,
					attachment
			);
		}

		private String normalizeReceiptType(@Nullable String receiptType) {
			return receiptType == null ? "DELIVERED" : receiptType.trim().toUpperCase(Locale.ROOT);
		}

		private int deliveryRank(String status) {
			return switch (status) {
				case "SENT" -> 1;
				case "DELIVERED" -> 2;
				case "READ" -> 3;
				case "RECALLED" -> 4;
				default -> 0;
			};
		}
	}
}
