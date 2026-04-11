package com.kzzz3.argus.cortex.conversation.domain;

import java.util.List;

public record ConversationDetail(
		String id,
		String title,
		String subtitle,
		int memberCount,
		List<String> memberDisplayNames
) {
}
