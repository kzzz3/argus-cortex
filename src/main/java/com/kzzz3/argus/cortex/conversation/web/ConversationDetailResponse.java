package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import java.util.List;

public record ConversationDetailResponse(
		String id,
		String title,
		String subtitle,
		int memberCount,
		List<String> memberDisplayNames
) {
	public static ConversationDetailResponse from(ConversationDetail detail) {
		return new ConversationDetailResponse(
				detail.id(),
				detail.title(),
				detail.subtitle(),
				detail.memberCount(),
				detail.memberDisplayNames()
		);
	}
}
