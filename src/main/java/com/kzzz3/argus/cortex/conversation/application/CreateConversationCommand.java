package com.kzzz3.argus.cortex.conversation.application;

public record CreateConversationCommand(
		String type,
		String title
) {
}
