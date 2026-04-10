package com.kzzz3.argus.cortex.conversation.domain;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.util.List;

public interface ConversationStore {

	List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays);

	ConversationMessagePage listMessages(
			AccountRecord accountRecord,
			String conversationId,
			int recentWindowDays,
			int limit,
			String sinceCursor
	);

	ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String body);

	ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId);

	ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId);
}
