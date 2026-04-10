package com.kzzz3.argus.cortex.conversation.domain;

public class MessageNotFoundException extends RuntimeException {

	public MessageNotFoundException(String messageId) {
		super("Message not found: " + messageId);
	}
}
