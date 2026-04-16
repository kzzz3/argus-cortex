package com.kzzz3.argus.cortex.friend.web;

import com.kzzz3.argus.cortex.friend.domain.PendingFriendRequests;
import java.util.List;

public record PendingFriendRequestsResponse(
		List<FriendRequestResponse> incoming,
		List<FriendRequestResponse> outgoing
) {
	public static PendingFriendRequestsResponse from(PendingFriendRequests pendingFriendRequests) {
		return new PendingFriendRequestsResponse(
				pendingFriendRequests.incoming().stream().map(FriendRequestResponse::from).toList(),
				pendingFriendRequests.outgoing().stream().map(FriendRequestResponse::from).toList()
		);
	}
}
