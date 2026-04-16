package com.kzzz3.argus.cortex.friend.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestNotFoundException;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.domain.PendingFriendRequests;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryFriendStore implements FriendStore {

	private final Map<String, LinkedHashMap<String, FriendRecord>> friendsByAccount = new ConcurrentHashMap<>();
	private final Map<String, PendingRequestState> pendingRequestsById = new ConcurrentHashMap<>();

	@Override
	public List<FriendRecord> listFriends(AccountRecord owner) {
		return new ArrayList<>(friendsByAccount.computeIfAbsent(owner.accountId(), ignored -> new LinkedHashMap<>()).values());
	}

	@Override
	public PendingFriendRequests listPendingRequests(AccountRecord owner) {
		List<FriendRequestRecord> incoming = pendingRequestsById.values().stream()
				.filter(request -> request.targetAccountId().equals(owner.accountId()))
				.map(request -> new FriendRequestRecord(
						request.requestId(),
						request.requesterAccountId(),
						request.requesterDisplayName(),
						"INCOMING",
						request.status(),
						request.note()
				))
				.toList();
		List<FriendRequestRecord> outgoing = pendingRequestsById.values().stream()
				.filter(request -> request.requesterAccountId().equals(owner.accountId()))
				.map(request -> new FriendRequestRecord(
						request.requestId(),
						request.targetAccountId(),
						request.targetDisplayName(),
						"OUTGOING",
						request.status(),
						request.note()
				))
				.toList();
		return new PendingFriendRequests(incoming, outgoing);
	}

	@Override
	public FriendRequestRecord sendFriendRequest(AccountRecord owner, AccountRecord friend) {
		LinkedHashMap<String, FriendRecord> ownerFriends = friendsByAccount.computeIfAbsent(owner.accountId(), ignored -> new LinkedHashMap<>());
		if (ownerFriends.containsKey(friend.accountId())) {
			throw new FriendAlreadyExistsException(friend.accountId());
		}

		boolean pendingExists = pendingRequestsById.values().stream()
				.anyMatch(request -> request.status().equals("PENDING") && (
						(request.requesterAccountId().equals(owner.accountId()) && request.targetAccountId().equals(friend.accountId()))
							|| (request.requesterAccountId().equals(friend.accountId()) && request.targetAccountId().equals(owner.accountId()))
				));
		if (pendingExists) {
			throw new FriendRequestAlreadyExistsException(friend.accountId());
		}

		String requestId = UUID.randomUUID().toString();
		PendingRequestState request = new PendingRequestState(
				requestId,
				owner.accountId(),
				owner.displayName(),
				friend.accountId(),
				friend.displayName(),
				"Sent from contact flow",
				"PENDING"
		);
		pendingRequestsById.put(requestId, request);
		return new FriendRequestRecord(requestId, friend.accountId(), friend.displayName(), "OUTGOING", "PENDING", request.note());
	}

	@Override
	public FriendRecord acceptFriendRequest(AccountRecord owner, String requestId) {
		PendingRequestState request = pendingRequestsById.get(requestId);
		if (request == null || !request.status().equals("PENDING") || !request.targetAccountId().equals(owner.accountId())) {
			throw new FriendRequestNotFoundException(requestId);
		}

		LinkedHashMap<String, FriendRecord> ownerFriends = friendsByAccount.computeIfAbsent(owner.accountId(), ignored -> new LinkedHashMap<>());
		if (ownerFriends.containsKey(request.requesterAccountId())) {
			pendingRequestsById.remove(requestId);
			throw new FriendAlreadyExistsException(request.requesterAccountId());
		}

		FriendRecord ownerView = new FriendRecord(request.requesterAccountId(), request.requesterDisplayName(), request.note());
		ownerFriends.put(request.requesterAccountId(), ownerView);
		LinkedHashMap<String, FriendRecord> friendFriends = friendsByAccount.computeIfAbsent(request.requesterAccountId(), ignored -> new LinkedHashMap<>());
		friendFriends.putIfAbsent(owner.accountId(), new FriendRecord(owner.accountId(), owner.displayName(), request.note()));
		pendingRequestsById.remove(requestId);
		return ownerView;
	}

	@Override
	public FriendRequestRecord rejectFriendRequest(AccountRecord owner, String requestId) {
		return resolvePendingRequest(owner, requestId, "REJECTED");
	}

	@Override
	public FriendRequestRecord ignoreFriendRequest(AccountRecord owner, String requestId) {
		return resolvePendingRequest(owner, requestId, "IGNORED");
	}

	private FriendRequestRecord resolvePendingRequest(AccountRecord owner, String requestId, String nextStatus) {
		PendingRequestState request = pendingRequestsById.get(requestId);
		if (request == null || !request.status().equals("PENDING") || !request.targetAccountId().equals(owner.accountId())) {
			throw new FriendRequestNotFoundException(requestId);
		}
		PendingRequestState updated = request.withStatus(nextStatus);
		pendingRequestsById.put(requestId, updated);
		return new FriendRequestRecord(
				updated.requestId(),
				updated.requesterAccountId(),
				updated.requesterDisplayName(),
				"INCOMING",
				updated.status(),
				updated.note()
		);
	}

	private record PendingRequestState(
			String requestId,
			String requesterAccountId,
			String requesterDisplayName,
			String targetAccountId,
			String targetDisplayName,
			String note,
			String status
	) {
		private PendingRequestState withStatus(String nextStatus) {
			return new PendingRequestState(
					requestId,
					requesterAccountId,
					requesterDisplayName,
					targetAccountId,
					targetDisplayName,
					note,
					nextStatus
			);
		}
	}
}
