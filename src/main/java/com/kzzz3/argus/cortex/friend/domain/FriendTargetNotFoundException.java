package com.kzzz3.argus.cortex.friend.domain;

public class FriendTargetNotFoundException extends RuntimeException {

	public FriendTargetNotFoundException(String accountId) {
		super("Friend target not found: " + accountId);
	}
}
