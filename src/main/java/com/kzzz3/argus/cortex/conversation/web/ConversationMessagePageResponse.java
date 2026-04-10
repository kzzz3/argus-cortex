package com.kzzz3.argus.cortex.conversation.web;

import java.util.List;

public record ConversationMessagePageResponse(
		List<ConversationMessageResponse> messages,
		String nextSyncCursor,
		int recentWindowDays,
		int limit
) {
}
