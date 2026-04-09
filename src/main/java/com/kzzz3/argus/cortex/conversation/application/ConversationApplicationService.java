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

	private final AccessTokenStore accessTokenStore;

	public ConversationApplicationService(AccessTokenStore accessTokenStore) {
		this.accessTokenStore = accessTokenStore;
	}

	public List<ConversationSummary> listConversations(String accessToken) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);

		return List.of(
				new ConversationSummary(
						"conv-zhang-san",
						"Zhang San",
						"1:1 direct chat",
						"Remote inbox is now wired from argus-cortex.",
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

	public List<ConversationMessage> listMessages(String accessToken, String conversationId) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);

		return switch (conversationId) {
			case "conv-zhang-san" -> List.of(
					new ConversationMessage(
							"msg-zhang-1",
							conversationId,
							"Zhang San",
							"Remote conversation messages are now flowing from argus-cortex.",
							"09:24",
							false,
							"DELIVERED"
					),
					new ConversationMessage(
							"msg-zhang-2",
							conversationId,
							accountRecord.displayName(),
							"This reply comes from the authenticated Android account.",
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
							"Next step: replace these seeded messages with real sync storage.",
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
							"Remote conversation placeholder for " + conversationId + ".",
							"Now",
							true,
							"SENT"
					)
			);
		};
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
}
