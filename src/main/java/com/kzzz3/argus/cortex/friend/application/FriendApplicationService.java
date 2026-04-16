package com.kzzz3.argus.cortex.friend.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.domain.FriendTargetNotFoundException;
import com.kzzz3.argus.cortex.friend.domain.PendingFriendRequests;
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

	public List<FriendRecord> listFriends() {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		return friendStore.listFriends(owner);
	}

	public PendingFriendRequests listPendingRequests() {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		return friendStore.listPendingRequests(owner);
	}

	@Transactional
	public FriendRequestRecord sendFriendRequest(AddFriendCommand request) {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		String friendAccountId = request.friendAccountId().trim();
		if (owner.accountId().equals(friendAccountId)) {
			throw new IllegalArgumentException("Cannot send a friend request to yourself.");
		}
		AccountRecord friend = accountStore.findByAccountId(friendAccountId)
				.orElseThrow(() -> new FriendTargetNotFoundException(request.friendAccountId()));
		return friendStore.sendFriendRequest(owner, friend);
	}

	@Transactional
	public FriendRecord acceptFriendRequest(AcceptFriendRequestCommand request) {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		FriendRecord friendRecord = friendStore.acceptFriendRequest(owner, request.requestId().trim());
		AccountRecord friend = accountStore.findByAccountId(friendRecord.accountId())
				.orElseThrow(() -> new FriendTargetNotFoundException(friendRecord.accountId()));
		conversationStore.ensureDirectConversation(owner, friend);
		return friendRecord;
	}

	@Transactional
	public FriendRequestRecord rejectFriendRequest(RejectFriendRequestCommand request) {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		return friendStore.rejectFriendRequest(owner, request.requestId().trim());
	}

	@Transactional
	public FriendRequestRecord ignoreFriendRequest(IgnoreFriendRequestCommand request) {
		AccountRecord owner = authenticatedAccountResolver.resolveCurrent();
		return friendStore.ignoreFriendRequest(owner, request.requestId().trim());
	}
}
