package com.kzzz3.argus.cortex.friend.domain;

public class FriendAlreadyExistsException extends RuntimeException {

	public FriendAlreadyExistsException(String accountId) {
		super("Friend already exists: " + accountId);
	}
}
