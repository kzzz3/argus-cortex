package com.kzzz3.argus.cortex.conversation.domain;

import java.util.List;

public record ConversationMessagePage(
		List<ConversationMessage> messages,
		String nextSyncCursor,
		int recentWindowDays,
		int limit
) {
}
