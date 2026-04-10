package com.kzzz3.argus.cortex.conversation.domain;

public record ConversationSummary(
		String id,
		String title,
		String subtitle,
		String preview,
		String timestampLabel,
		int unreadCount,
		String syncCursor
) {
}
