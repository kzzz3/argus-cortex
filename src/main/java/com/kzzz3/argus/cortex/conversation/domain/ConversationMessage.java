package com.kzzz3.argus.cortex.conversation.domain;

public record ConversationMessage(
		String id,
		String conversationId,
		String senderDisplayName,
		String body,
		String timestampLabel,
		boolean fromCurrentUser,
		String deliveryStatus
) {
}
