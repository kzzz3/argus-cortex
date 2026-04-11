package com.kzzz3.argus.cortex.friend.web;

import com.kzzz3.argus.cortex.friend.domain.FriendRecord;

public record FriendResponse(
		String accountId,
		String displayName,
		String note
) {
	public static FriendResponse from(FriendRecord friendRecord) {
		return new FriendResponse(
				friendRecord.accountId(),
				friendRecord.displayName(),
				friendRecord.note()
		);
	}
}
