package com.kzzz3.argus.cortex.friend.web;

import com.kzzz3.argus.cortex.friend.domain.FriendRequestRecord;

public record FriendRequestResponse(
		String requestId,
		String accountId,
		String displayName,
		String direction,
		String status,
		String note
) {
	public static FriendRequestResponse from(FriendRequestRecord record) {
		return new FriendRequestResponse(
				record.requestId(),
				record.accountId(),
				record.displayName(),
				record.direction(),
				record.status(),
				record.note()
		);
	}
}
