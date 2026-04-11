package com.kzzz3.argus.cortex.friend.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryFriendStore implements FriendStore {

	private final Map<String, LinkedHashMap<String, FriendRecord>> friendsByAccount = new ConcurrentHashMap<>();

	@Override
	public List<FriendRecord> listFriends(AccountRecord owner) {
		return new ArrayList<>(friendsByAccount.computeIfAbsent(owner.accountId(), ignored -> seedFriends(owner)).values());
	}

	@Override
	public FriendRecord addFriend(AccountRecord owner, AccountRecord friend) {
		LinkedHashMap<String, FriendRecord> ownerFriends = friendsByAccount.computeIfAbsent(owner.accountId(), ignored -> seedFriends(owner));
		if (ownerFriends.containsKey(friend.accountId())) {
			throw new FriendAlreadyExistsException(friend.accountId());
		}

		FriendRecord ownerView = new FriendRecord(friend.accountId(), friend.displayName(), "Added from remote contact flow");
		ownerFriends.put(friend.accountId(), ownerView);

		LinkedHashMap<String, FriendRecord> friendFriends = friendsByAccount.computeIfAbsent(friend.accountId(), ignored -> new LinkedHashMap<>());
		friendFriends.putIfAbsent(owner.accountId(), new FriendRecord(owner.accountId(), owner.displayName(), "Added from remote contact flow"));

		return ownerView;
	}

	private LinkedHashMap<String, FriendRecord> seedFriends(AccountRecord owner) {
		LinkedHashMap<String, FriendRecord> seeded = new LinkedHashMap<>();
		if (!owner.accountId().equals("zhangsan")) {
			seeded.put("zhangsan", new FriendRecord("zhangsan", "Zhang San", "Default remote contact"));
		}
		return seeded;
	}
}
