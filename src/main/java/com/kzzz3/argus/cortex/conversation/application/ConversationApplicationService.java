package com.kzzz3.argus.cortex.conversation.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import java.util.List;
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
}
