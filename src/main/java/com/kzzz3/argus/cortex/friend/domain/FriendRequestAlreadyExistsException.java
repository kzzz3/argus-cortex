package com.kzzz3.argus.cortex.friend.domain;

public class FriendRequestAlreadyExistsException extends RuntimeException {

	public FriendRequestAlreadyExistsException(String accountId) {
		super("Pending friend request already exists for account: " + accountId);
	}
}
