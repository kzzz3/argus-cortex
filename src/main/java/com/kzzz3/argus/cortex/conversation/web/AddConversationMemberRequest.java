package com.kzzz3.argus.cortex.conversation.web;

import jakarta.validation.constraints.NotBlank;

public record AddConversationMemberRequest(
		@NotBlank String memberAccountId
) {
}
