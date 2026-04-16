package com.kzzz3.argus.cortex.conversation.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryConversationStore implements ConversationStore {

	private final Map<String, LinkedHashMap<String, ConversationThreadState>> conversationsByAccount = new ConcurrentHashMap<>();

	@Override
	public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
		return getOrCreateConversations(accountRecord).values().stream()
				.map(ConversationThreadState::toSummary)
				.toList();
	}

	@Override
	public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
		return threadState.toDetail(accountRecord.displayName());
	}

	@Override
	public ConversationMessagePage listMessages(
			AccountRecord accountRecord,
			String conversationId,
			int recentWindowDays,
			int limit,
			String sinceCursor
	) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
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
				List<ConversationMessage> continuationMessages = threadState.messagesAfter(requestedCursor.sequence(), limit);
				String nextCursor = continuationMessages.isEmpty()
						? threadState.cursor()
						: ConversationSyncCursor.of(conversationId, requestedCursor.sequence() + continuationMessages.size(), currentCursor.revision()).encoded();
				return new ConversationMessagePage(continuationMessages, nextCursor, recentWindowDays, limit);
			}
		}

		List<ConversationMessage> recentMessages = threadState.recentMessages(limit);
		return new ConversationMessagePage(recentMessages, threadState.cursor(), recentWindowDays, limit);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body, @Nullable ConversationMessageAttachment attachment) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
		if (clientMessageId != null && !clientMessageId.isBlank()) {
			ConversationMessage existing = threadState.findByClientMessageId(clientMessageId);
			if (existing != null) {
				return existing;
			}
		}
		ConversationMessage sentMessage = new ConversationMessage(
				clientMessageId == null || clientMessageId.isBlank() ? "msg-" + UUID.randomUUID() : clientMessageId,
				conversationId,
				accountRecord.displayName(),
				body.trim(),
				"Now",
				true,
				"DELIVERED",
				"2026-04-10T21:10:00+08:00",
				attachment
		);
		threadState.addMessage(sentMessage);
		return sentMessage;
	}

	@Override
	public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
		return threadState.applyReceipt(messageId, receiptType);
	}

	@Override
	public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
		return threadState.recallMessage(accountRecord.displayName(), messageId);
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId);
		threadState.markRead();
		return threadState.toSummary();
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		ConversationThreadState ownerThread = getOrCreateConversations(owner)
				.computeIfAbsent(conversationId, ignored -> new ConversationThreadState(
						conversationId,
						friend.displayName(),
						"Direct friend conversation",
						0,
						new ArrayList<>(),
						new ArrayList<>(List.of(owner.displayName(), friend.displayName()))
				));
		getOrCreateConversations(friend)
				.computeIfAbsent(conversationId, ignored -> new ConversationThreadState(
						conversationId,
						owner.displayName(),
						"Direct friend conversation",
						0,
						new ArrayList<>(),
						new ArrayList<>(List.of(owner.displayName(), friend.displayName()))
				));
		return ownerThread.toSummary();
	}

	private ConversationThreadState requireConversation(AccountRecord accountRecord, String conversationId) {
		ConversationThreadState threadState = getOrCreateConversations(accountRecord).get(conversationId);
		if (threadState == null) {
			throw new ConversationNotFoundException(conversationId);
		}
		return threadState;
	}

	private LinkedHashMap<String, ConversationThreadState> getOrCreateConversations(AccountRecord accountRecord) {
		return conversationsByAccount.computeIfAbsent(accountRecord.accountId(), ignored -> new LinkedHashMap<>());
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}

	private static final class ConversationThreadState {
		private final String id;
		private final String title;
		private final String subtitle;
		private final List<ConversationMessage> messages;
		private final List<String> members;
		private int unreadCount;
		private long latestMessageSequence;
		private long lastAffectedSequence;
		private long syncRevision;

		private ConversationThreadState(String id, String title, String subtitle, int unreadCount, List<ConversationMessage> messages, List<String> members) {
			this.id = id;
			this.title = title;
			this.subtitle = subtitle;
			this.unreadCount = unreadCount;
			this.messages = messages;
			this.members = members;
			this.latestMessageSequence = messages.size();
			this.lastAffectedSequence = latestMessageSequence;
			this.syncRevision = latestMessageSequence;
		}

		private String preview() {
			return messages.isEmpty() ? "No messages yet" : messages.getLast().body();
		}

		private String timestampLabel() {
			return messages.isEmpty() ? "--:--" : messages.getLast().timestampLabel();
		}

		private String cursor() {
			return ConversationSyncCursor.of(id, lastAffectedSequence, syncRevision).encoded();
		}

		private ConversationSummary toSummary() {
			return new ConversationSummary(id, title, subtitle, preview(), timestampLabel(), unreadCount, cursor());
		}

		private ConversationDetail toDetail(String ownerDisplayName) {
			List<String> memberNames = members.isEmpty()
					? (id.startsWith("conv-group-") ? List.of(ownerDisplayName, "Zhang San", "Li Si") : List.of(ownerDisplayName, title))
					: members;
			return new ConversationDetail(id, title, subtitle, memberNames.size(), memberNames);
		}

		private void ensureMember(String displayName) {
			if (!members.contains(displayName)) {
				members.add(displayName);
			}
		}

		private void addMessage(ConversationMessage message) {
			messages.add(message);
			latestMessageSequence += 1;
			lastAffectedSequence = latestMessageSequence;
			syncRevision += 1;
		}

		private void markRead() {
			unreadCount = 0;
			lastAffectedSequence = Math.max(lastAffectedSequence, latestMessageSequence);
			syncRevision += 1;
		}

		private ConversationMessage recallMessage(String displayName, String messageId) {
			for (int index = 0; index < messages.size(); index += 1) {
				ConversationMessage message = messages.get(index);
				if (message.id().equals(messageId)) {
					ConversationMessage recalled = new ConversationMessage(
							message.id(),
							message.conversationId(),
							displayName,
							"You recalled a message",
							"Now",
							true,
							"RECALLED",
							"2026-04-10T21:11:00+08:00",
							message.attachment()
					);
					messages.set(index, recalled);
					lastAffectedSequence = Math.max(lastAffectedSequence, index + 1L);
					syncRevision += 1;
					return recalled;
				}
			}
			throw new MessageNotFoundException(messageId);
		}

		private ConversationMessage applyReceipt(String messageId, String receiptType) {
			for (int index = 0; index < messages.size(); index += 1) {
				ConversationMessage message = messages.get(index);
				if (message.id().equals(messageId)) {
					String normalizedReceiptType = receiptType == null ? "DELIVERED" : receiptType.toUpperCase();
					String nextStatus = switch (normalizedReceiptType) {
						case "READ" -> "READ";
						case "DELIVERED" -> "DELIVERED";
						default -> "DELIVERED";
					};
					ConversationMessage updated = new ConversationMessage(
							message.id(),
							message.conversationId(),
							message.senderDisplayName(),
							message.body(),
							message.timestampLabel(),
							message.fromCurrentUser(),
							nextStatus,
							"2026-04-10T22:30:00+08:00",
							message.attachment()
					);
					messages.set(index, updated);
					lastAffectedSequence = Math.max(lastAffectedSequence, index + 1L);
					syncRevision += 1;
					return updated;
				}
			}
			throw new MessageNotFoundException(messageId);
		}

		private List<ConversationMessage> recentMessages(int limit) {
			int fromIndex = Math.max(messages.size() - limit, 0);
			return new ArrayList<>(messages.subList(fromIndex, messages.size()));
		}

		private List<ConversationMessage> messagesAfter(long sequence, int limit) {
			int fromIndex = Math.min(Math.max((int) sequence, 0), messages.size());
			int toIndex = Math.min(fromIndex + limit, messages.size());
			return new ArrayList<>(messages.subList(fromIndex, toIndex));
		}

		private ConversationMessage findByClientMessageId(String clientMessageId) {
			return messages.stream()
					.filter(message -> message.id().equals(clientMessageId))
					.findFirst()
					.orElse(null);
		}
	}
}
