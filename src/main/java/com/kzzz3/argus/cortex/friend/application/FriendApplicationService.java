package com.kzzz3.argus.cortex.friend.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.domain.FriendTargetNotFoundException;
import com.kzzz3.argus.cortex.friend.web.AddFriendRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FriendApplicationService {

	private final AccessTokenStore accessTokenStore;
	private final AccountStore accountStore;
	private final FriendStore friendStore;
	private final ConversationStore conversationStore;

	public FriendApplicationService(
			AccessTokenStore accessTokenStore,
			AccountStore accountStore,
			FriendStore friendStore,
			ConversationStore conversationStore
	) {
		this.accessTokenStore = accessTokenStore;
		this.accountStore = accountStore;
		this.friendStore = friendStore;
		this.conversationStore = conversationStore;
	}

	public List<FriendRecord> listFriends(String accessToken) {
		AccountRecord owner = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return friendStore.listFriends(owner);
	}

	public FriendRecord addFriend(String accessToken, AddFriendRequest request) {
		AccountRecord owner = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		AccountRecord friend = accountStore.findByAccountId(request.friendAccountId().trim())
				.orElseThrow(() -> new FriendTargetNotFoundException(request.friendAccountId()));
		FriendRecord friendRecord = friendStore.addFriend(owner, friend);
		conversationStore.ensureDirectConversation(owner, friend);
		return friendRecord;
	}
}
