package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;

public record ConversationSummaryResponse(
		String id,
		String title,
		String subtitle,
		String preview,
		String timestampLabel,
		int unreadCount,
		String syncCursor
) {
	public static ConversationSummaryResponse from(ConversationSummary summary) {
		return new ConversationSummaryResponse(
				summary.id(),
				summary.title(),
				summary.subtitle(),
				summary.preview(),
				summary.timestampLabel(),
				summary.unreadCount(),
				summary.syncCursor()
		);
	}
}
