package com.kzzz3.argus.cortex.friend.domain;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.util.List;

public interface FriendStore {

	List<FriendRecord> listFriends(AccountRecord owner);

	PendingFriendRequests listPendingRequests(AccountRecord owner);

	FriendRequestRecord sendFriendRequest(AccountRecord owner, AccountRecord friend);

	FriendRecord acceptFriendRequest(AccountRecord owner, String requestId);

	FriendRequestRecord rejectFriendRequest(AccountRecord owner, String requestId);

	FriendRequestRecord ignoreFriendRequest(AccountRecord owner, String requestId);
}
