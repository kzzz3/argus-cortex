package com.kzzz3.argus.cortex.friend.domain;

public class FriendRequestNotFoundException extends RuntimeException {

	public FriendRequestNotFoundException(String requestId) {
		super("Friend request not found: " + requestId);
	}
}
