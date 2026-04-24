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
		@Nullable ConversationMessageAttachment attachment,
		boolean duplicateClientMessage
) {
	public ConversationMessage(
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
		this(id, conversationId, senderDisplayName, body, timestampLabel, fromCurrentUser, deliveryStatus, statusUpdatedAt, attachment, false);
	}
}
