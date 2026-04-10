package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;

public record ConversationMessageResponse(
		String id,
		String conversationId,
		String senderDisplayName,
		String body,
		String timestampLabel,
		boolean fromCurrentUser,
		String deliveryStatus,
		String statusUpdatedAt
) {
	public static ConversationMessageResponse from(ConversationMessage message) {
		return new ConversationMessageResponse(
				message.id(),
				message.conversationId(),
				message.senderDisplayName(),
				message.body(),
				message.timestampLabel(),
				message.fromCurrentUser(),
				message.deliveryStatus(),
				message.statusUpdatedAt()
		);
	}
}
