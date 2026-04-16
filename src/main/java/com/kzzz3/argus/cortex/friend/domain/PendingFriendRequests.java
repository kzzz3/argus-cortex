package com.kzzz3.argus.cortex.friend.domain;

import java.util.List;

public record PendingFriendRequests(
		List<FriendRequestRecord> incoming,
		List<FriendRequestRecord> outgoing
) {
}
