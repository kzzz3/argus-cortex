package com.kzzz3.argus.cortex.friend.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.domain.FriendTargetNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendApplicationService {

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final AccountStore accountStore;
	private final FriendStore friendStore;
	private final ConversationStore conversationStore;

	public FriendApplicationService(
			AuthenticatedAccountResolver authenticatedAccountResolver,
			AccountStore accountStore,
			FriendStore friendStore,
			ConversationStore conversationStore
	) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
		this.accountStore = accountStore;
		this.friendStore = friendStore;
		this.conversationStore = conversationStore;
	}

	public List<FriendRecord> listFriends(String accessToken) {
		AccountRecord owner = authenticatedAccountResolver.resolve(accessToken);
		return friendStore.listFriends(owner);
	}

	@Transactional
	public FriendRecord addFriend(String accessToken, AddFriendCommand request) {
		AccountRecord owner = authenticatedAccountResolver.resolve(accessToken);
		AccountRecord friend = accountStore.findByAccountId(request.friendAccountId().trim())
				.orElseThrow(() -> new FriendTargetNotFoundException(request.friendAccountId()));
		FriendRecord friendRecord = friendStore.addFriend(owner, friend);
		conversationStore.ensureDirectConversation(owner, friend);
		return friendRecord;
	}
}
