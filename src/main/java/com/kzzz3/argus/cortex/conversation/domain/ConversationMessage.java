package com.kzzz3.argus.cortex.conversation.domain;

import org.jspecify.annotations.Nullable;

public record ConversationMessage(
		String id,
		String conversationId,
		String senderDisplayName,
		String body,
		String timestampLabel,
		boolean fromCurrentUser,
		String deliveryStatus,
		String statusUpdatedAt,
		@Nullable ConversationMessageAttachment attachment
) {
}
