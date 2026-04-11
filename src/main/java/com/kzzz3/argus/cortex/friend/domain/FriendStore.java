package com.kzzz3.argus.cortex.friend.domain;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.util.List;

public interface FriendStore {

	List<FriendRecord> listFriends(AccountRecord owner);

	FriendRecord addFriend(AccountRecord owner, AccountRecord friend);
}
