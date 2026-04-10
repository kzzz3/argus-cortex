package com.kzzz3.argus.cortex.conversation.domain;

public class ConversationNotFoundException extends RuntimeException {

	public ConversationNotFoundException(String conversationId) {
		super("Conversation not found: " + conversationId);
	}
}
