package com.kzzz3.argus.cortex.conversation.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record SendMessageRequest(
		@NotBlank String clientMessageId,
		@Nullable String body,
		@Nullable @Valid ConversationMessageAttachmentRequest attachment
) {
}
