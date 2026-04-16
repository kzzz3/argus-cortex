package com.kzzz3.argus.cortex.friend.domain;

public record FriendRequestRecord(
		String requestId,
		String accountId,
		String displayName,
		String direction,
		String status,
		String note
) {
}
