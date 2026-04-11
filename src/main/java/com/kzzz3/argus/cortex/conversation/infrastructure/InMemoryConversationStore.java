package com.kzzz3.argus.cortex.conversation.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryConversationStore implements ConversationStore {

	private final Map<String, LinkedHashMap<String, ConversationThreadState>> conversationsByAccount = new ConcurrentHashMap<>();

	@Override
	public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
		return getOrSeedConversations(accountRecord, recentWindowDays).values().stream()
				.map(ConversationThreadState::toSummary)
				.toList();
	}

	@Override
	public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, 7);
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
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, recentWindowDays);
		if (threadState.cursor().equals(sinceCursor)) {
			return new ConversationMessagePage(List.of(), threadState.cursor(), recentWindowDays, limit);
		}

		List<ConversationMessage> recentMessages = threadState.recentMessages(limit);
		return new ConversationMessagePage(recentMessages, threadState.cursor(), recentWindowDays, limit);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, 7);
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
				"2026-04-10T21:10:00+08:00"
		);
		threadState.addMessage(sentMessage);
		return sentMessage;
	}

	@Override
	public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, 7);
		return threadState.applyReceipt(messageId, receiptType);
	}

	@Override
	public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, 7);
		return threadState.recallMessage(accountRecord.displayName(), messageId);
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		ConversationThreadState threadState = requireConversation(accountRecord, conversationId, 7);
		threadState.markRead();
		return threadState.toSummary();
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		ConversationThreadState ownerThread = getOrSeedConversations(owner, 7)
				.computeIfAbsent(conversationId, ignored -> new ConversationThreadState(
						conversationId,
						friend.displayName(),
						"Direct friend conversation",
						0,
						new ArrayList<>(),
						new ArrayList<>(List.of(owner.displayName(), friend.displayName()))
				));
		getOrSeedConversations(friend, 7)
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

	@Override
	public ConversationSummary createConversation(AccountRecord owner, String type, String title) {
		String normalizedType = type == null ? "GROUP" : type.trim().toUpperCase();
		String normalizedTitle = title == null || title.isBlank() ? "Untitled Conversation" : title.trim();
		String conversationId = switch (normalizedType) {
			case "DIRECT" -> "conv-direct-" + owner.accountId() + "-self";
			case "GROUP" -> "conv-group-" + UUID.randomUUID();
			default -> "conv-group-" + UUID.randomUUID();
		};
		ConversationThreadState threadState = getOrSeedConversations(owner, 7)
				.computeIfAbsent(conversationId, ignored -> new ConversationThreadState(
						conversationId,
						normalizedTitle,
						normalizedType.equals("GROUP") ? "Remote group conversation" : "Remote direct conversation",
						0,
						new ArrayList<>(),
						new ArrayList<>(List.of(owner.displayName()))
				));
		return threadState.toSummary();
	}

	@Override
	public ConversationDetail addMember(AccountRecord owner, String conversationId, AccountRecord member) {
		ConversationThreadState ownerThread = requireConversation(owner, conversationId, 7);
		ownerThread.ensureMember(member.displayName());

		ConversationThreadState memberThread = getOrSeedConversations(member, 7)
				.computeIfAbsent(conversationId, ignored -> new ConversationThreadState(
						conversationId,
						ownerThread.title,
						ownerThread.subtitle,
						0,
						new ArrayList<>(),
						new ArrayList<>(ownerThread.members)
				));
		memberThread.ensureMember(member.displayName());

		for (Map.Entry<String, LinkedHashMap<String, ConversationThreadState>> entry : conversationsByAccount.entrySet()) {
			ConversationThreadState threadState = entry.getValue().get(conversationId);
			if (threadState != null) {
				threadState.ensureMember(member.displayName());
			}
		}

		return ownerThread.toDetail(owner.displayName());
	}

	private ConversationThreadState requireConversation(AccountRecord accountRecord, String conversationId, int recentWindowDays) {
		ConversationThreadState threadState = getOrSeedConversations(accountRecord, recentWindowDays).get(conversationId);
		if (threadState == null) {
			throw new ConversationNotFoundException(conversationId);
		}
		return threadState;
	}

	private LinkedHashMap<String, ConversationThreadState> getOrSeedConversations(AccountRecord accountRecord, int recentWindowDays) {
		return conversationsByAccount.computeIfAbsent(accountRecord.accountId(), ignored -> seedConversations(accountRecord, recentWindowDays));
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}

	private LinkedHashMap<String, ConversationThreadState> seedConversations(AccountRecord accountRecord, int recentWindowDays) {
		LinkedHashMap<String, ConversationThreadState> threads = new LinkedHashMap<>();

		threads.put("conv-zhang-san", new ConversationThreadState(
				"conv-zhang-san",
				"Zhang San",
				"1:1 direct chat",
				2,
				new ArrayList<>(List.of(
						new ConversationMessage(
								"msg-zhang-1",
								"conv-zhang-san",
								"Zhang San",
								"Remote message sync currently serves a recent " + recentWindowDays + "-day window.",
								"09:24",
								false,
								"DELIVERED",
								"2026-04-10T09:24:00+08:00"
						),
						new ConversationMessage(
								"msg-zhang-2",
								"conv-zhang-san",
								accountRecord.displayName(),
								"This reply comes from the authenticated Android account inside the remote recent window.",
								"09:28",
								true,
								"DELIVERED",
								"2026-04-10T09:28:00+08:00"
						)
				)),
				new ArrayList<>(List.of(accountRecord.displayName(), "Zhang San"))
		));

		threads.put("conv-project-group", new ConversationThreadState(
				"conv-project-group",
				"Project Group",
				"3 members",
				0,
				new ArrayList<>(List.of(
						new ConversationMessage(
								"msg-group-1",
								"conv-project-group",
								"Project Group",
								"Next step: replace seeded windowed messages with real sync storage.",
								"Yesterday",
								false,
								"DELIVERED",
								"2026-04-09T20:00:00+08:00"
						)
				)),
				new ArrayList<>(List.of(accountRecord.displayName(), "Zhang San", "Li Si"))
		));

		threads.put("conv-li-si", new ConversationThreadState(
				"conv-li-si",
				"Li Si",
				"Feature review",
				1,
				new ArrayList<>(List.of(
						new ConversationMessage(
								"msg-li-si-1",
								"conv-li-si",
								"Li Si",
								"Cursor-based sync is the next realistic IM step.",
								"Mon",
								false,
								"SENT",
								"2026-04-08T10:00:00+08:00"
						)
				)),
				new ArrayList<>(List.of(accountRecord.displayName(), "Li Si"))
		));

		return threads;
	}

	private static final class ConversationThreadState {
		private final String id;
		private final String title;
		private final String subtitle;
		private final List<ConversationMessage> messages;
		private final List<String> members;
		private int unreadCount;
		private long version;

		private ConversationThreadState(String id, String title, String subtitle, int unreadCount, List<ConversationMessage> messages, List<String> members) {
			this.id = id;
			this.title = title;
			this.subtitle = subtitle;
			this.unreadCount = unreadCount;
			this.messages = messages;
			this.members = members;
			this.version = messages.size();
		}

		private String preview() {
			return messages.isEmpty() ? "No messages yet" : messages.getLast().body();
		}

		private String timestampLabel() {
			return messages.isEmpty() ? "--:--" : messages.getLast().timestampLabel();
		}

		private String cursor() {
			return "cursor-" + id + "-" + version;
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
				version += 1;
			}
		}

		private void addMessage(ConversationMessage message) {
			messages.add(message);
			version += 1;
		}

		private void markRead() {
			unreadCount = 0;
			version += 1;
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
							"2026-04-10T21:11:00+08:00"
					);
					messages.set(index, recalled);
					version += 1;
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
							"2026-04-10T22:30:00+08:00"
					);
					messages.set(index, updated);
					version += 1;
					return updated;
				}
			}
			throw new MessageNotFoundException(messageId);
		}

		private List<ConversationMessage> recentMessages(int limit) {
			int fromIndex = Math.max(messages.size() - limit, 0);
			return messages.subList(fromIndex, messages.size());
		}

		private ConversationMessage findByClientMessageId(String clientMessageId) {
			return messages.stream()
					.filter(message -> message.id().equals(clientMessageId))
					.findFirst()
					.orElse(null);
		}
	}
}
