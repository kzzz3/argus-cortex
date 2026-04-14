package com.kzzz3.argus.cortex.conversation.application;

import org.jspecify.annotations.Nullable;

public record SendMessageCommand(
		String clientMessageId,
		@Nullable String body,
		@Nullable String attachmentId
) {
}
