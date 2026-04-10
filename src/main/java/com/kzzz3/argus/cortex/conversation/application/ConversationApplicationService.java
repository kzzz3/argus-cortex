package com.kzzz3.argus.cortex.conversation.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.web.SendMessageRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConversationApplicationService {

	private static final int DEFAULT_RECENT_WINDOW_DAYS = 7;
	private static final int DEFAULT_MESSAGE_LIMIT = 50;

	private final AccessTokenStore accessTokenStore;

	public ConversationApplicationService(AccessTokenStore accessTokenStore) {
		this.accessTokenStore = accessTokenStore;
	}

	public List<ConversationSummary> listConversations(String accessToken, int recentWindowDays) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);

		return List.of(
				new ConversationSummary(
						"conv-zhang-san",
						"Zhang San",
						"1:1 direct chat",
						"Remote inbox keeps only the latest " + normalizedWindowDays + " days in server scope.",
						"09:24",
						2
				),
				new ConversationSummary(
						"conv-project-group",
						"Project Group",
						"3 members",
						accountRecord.displayName() + " is now reading a remote conversation list.",
						"Yesterday",
						0
				),
				new ConversationSummary(
						"conv-li-si",
						"Li Si",
						"Feature review",
						"Next step: wire message diff sync on top of this remote thread list.",
						"Mon",
						1
				)
		);
	}

	public List<ConversationMessage> listMessages(String accessToken, String conversationId, int recentWindowDays, int limit) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
		int normalizedLimit = normalizeMessageLimit(limit);

		List<ConversationMessage> windowedMessages = switch (conversationId) {
			case "conv-zhang-san" -> List.of(
					new ConversationMessage(
							"msg-zhang-1",
							conversationId,
							"Zhang San",
							"Remote message sync currently serves a recent " + normalizedWindowDays + "-day window.",
							"09:24",
							false,
							"DELIVERED"
					),
					new ConversationMessage(
							"msg-zhang-2",
							conversationId,
							accountRecord.displayName(),
							"This reply comes from the authenticated Android account inside the remote recent window.",
							"09:28",
							true,
							"DELIVERED"
					)
			);
			case "conv-project-group" -> List.of(
					new ConversationMessage(
							"msg-group-1",
							conversationId,
							"Project Group",
							"Next step: replace seeded windowed messages with real sync storage.",
							"Yesterday",
							false,
							"DELIVERED"
					)
			);
			default -> List.of(
					new ConversationMessage(
							"msg-generic-1",
							conversationId,
							accountRecord.displayName(),
							"Remote recent-window placeholder for " + conversationId + ".",
							"Now",
							true,
							"SENT"
					)
			);
		};

		return windowedMessages.stream().limit(normalizedLimit).toList();
	}

	public ConversationMessage sendMessage(
			String accessToken,
			String conversationId,
			SendMessageRequest request
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);

		return new ConversationMessage(
				"msg-" + UUID.randomUUID(),
				conversationId,
				accountRecord.displayName(),
				request.body().trim(),
				"Now",
				true,
				"DELIVERED"
		);
	}

	public ConversationMessage recallMessage(
			String accessToken,
			String conversationId,
			String messageId
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);

		return new ConversationMessage(
				messageId,
				conversationId,
				accountRecord.displayName(),
				"You recalled a message",
				"Now",
				true,
				"RECALLED"
		);
	}

	public ConversationSummary markConversationRead(
			String accessToken,
			String conversationId
	) {
		accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);

		return switch (conversationId) {
			case "conv-zhang-san" -> new ConversationSummary(
					"conv-zhang-san",
					"Zhang San",
					"1:1 direct chat",
					"Remote inbox keeps only the latest 7 days in server scope.",
					"09:24",
					0
			);
			case "conv-project-group" -> new ConversationSummary(
					"conv-project-group",
					"Project Group",
					"3 members",
					"Conversation has been marked read on the server side.",
					"Yesterday",
					0
			);
			default -> new ConversationSummary(
					conversationId,
					conversationId,
					"Remote conversation",
					"Conversation has been marked read.",
					"Now",
					0
			);
		};
	}

	private int normalizeRecentWindowDays(int requestedWindowDays) {
		return requestedWindowDays <= 0 ? DEFAULT_RECENT_WINDOW_DAYS : Math.min(requestedWindowDays, 30);
	}

	private int normalizeMessageLimit(int requestedLimit) {
		return requestedLimit <= 0 ? DEFAULT_MESSAGE_LIMIT : Math.min(requestedLimit, 200);
	}
}
