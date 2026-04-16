package com.kzzz3.argus.cortex.friend.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AccountEntity;
import com.kzzz3.argus.cortex.auth.infrastructure.mapper.AccountMapper;
import com.kzzz3.argus.cortex.friend.domain.FriendAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestNotFoundException;
import com.kzzz3.argus.cortex.friend.domain.FriendRequestRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.domain.PendingFriendRequests;
import com.kzzz3.argus.cortex.friend.infrastructure.entity.FriendRequestEntity;
import com.kzzz3.argus.cortex.friend.infrastructure.entity.FriendRelationEntity;
import com.kzzz3.argus.cortex.friend.infrastructure.mapper.FriendRequestMapper;
import com.kzzz3.argus.cortex.friend.infrastructure.mapper.FriendRelationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisFriendStore implements FriendStore {

	private final FriendRelationMapper friendRelationMapper;
	private final FriendRequestMapper friendRequestMapper;
	private final AccountMapper accountMapper;

	public MybatisFriendStore(FriendRelationMapper friendRelationMapper, FriendRequestMapper friendRequestMapper, AccountMapper accountMapper) {
		this.friendRelationMapper = friendRelationMapper;
		this.friendRequestMapper = friendRequestMapper;
		this.accountMapper = accountMapper;
	}

	@Override
	public List<FriendRecord> listFriends(AccountRecord owner) {
		return friendRelationMapper.selectList(new LambdaQueryWrapper<FriendRelationEntity>()
				.eq(FriendRelationEntity::getOwnerAccountId, owner.accountId())
				.orderByAsc(FriendRelationEntity::getId))
				.stream()
				.map(entity -> new FriendRecord(
						entity.getFriendAccountId(),
						resolveDisplayName(entity.getFriendAccountId()),
						entity.getNote()
				))
				.toList();
	}

	@Override
	public PendingFriendRequests listPendingRequests(AccountRecord owner) {
		List<FriendRequestRecord> incoming = friendRequestMapper.selectList(new LambdaQueryWrapper<FriendRequestEntity>()
				.eq(FriendRequestEntity::getTargetAccountId, owner.accountId())
				.eq(FriendRequestEntity::getStatus, "PENDING")
				.orderByDesc(FriendRequestEntity::getCreatedAt))
				.stream()
				.map(entity -> new FriendRequestRecord(
						entity.getRequestId(),
						entity.getRequesterAccountId(),
						entity.getRequesterDisplayName(),
						"INCOMING",
						entity.getStatus(),
						entity.getNote()
				))
				.toList();
		List<FriendRequestRecord> outgoing = friendRequestMapper.selectList(new LambdaQueryWrapper<FriendRequestEntity>()
				.eq(FriendRequestEntity::getRequesterAccountId, owner.accountId())
				.orderByDesc(FriendRequestEntity::getCreatedAt))
				.stream()
				.map(entity -> new FriendRequestRecord(
						entity.getRequestId(),
						entity.getTargetAccountId(),
						entity.getTargetDisplayName(),
						"OUTGOING",
						entity.getStatus(),
						entity.getNote()
				))
				.toList();
		return new PendingFriendRequests(incoming, outgoing);
	}

	@Override
	public FriendRequestRecord sendFriendRequest(AccountRecord owner, AccountRecord friend) {
		boolean exists = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelationEntity>()
				.eq(FriendRelationEntity::getOwnerAccountId, owner.accountId())
				.eq(FriendRelationEntity::getFriendAccountId, friend.accountId())) > 0;
		if (exists) {
			throw new FriendAlreadyExistsException(friend.accountId());
		}
		boolean pendingExists = friendRequestMapper.selectCount(new LambdaQueryWrapper<FriendRequestEntity>()
				.eq(FriendRequestEntity::getStatus, "PENDING")
				.and(wrapper -> wrapper
						.and(inner -> inner.eq(FriendRequestEntity::getRequesterAccountId, owner.accountId())
								.eq(FriendRequestEntity::getTargetAccountId, friend.accountId()))
						.or(inner -> inner.eq(FriendRequestEntity::getRequesterAccountId, friend.accountId())
								.eq(FriendRequestEntity::getTargetAccountId, owner.accountId()))
				)) > 0;
		if (pendingExists) {
			throw new FriendRequestAlreadyExistsException(friend.accountId());
		}

		String requestId = UUID.randomUUID().toString();
		FriendRequestEntity entity = new FriendRequestEntity();
		entity.setRequestId(requestId);
		entity.setRequesterAccountId(owner.accountId());
		entity.setRequesterDisplayName(owner.displayName());
		entity.setTargetAccountId(friend.accountId());
		entity.setTargetDisplayName(friend.displayName());
		entity.setNote("Sent from contact flow");
		entity.setStatus("PENDING");
		entity.setCreatedAt(LocalDateTime.now());
		friendRequestMapper.insert(entity);
		return new FriendRequestRecord(requestId, friend.accountId(), friend.displayName(), "OUTGOING", "PENDING", entity.getNote());
	}

	@Override
	public FriendRecord acceptFriendRequest(AccountRecord owner, String requestId) {
		FriendRequestEntity request = friendRequestMapper.selectOne(new LambdaQueryWrapper<FriendRequestEntity>()
				.eq(FriendRequestEntity::getRequestId, requestId)
				.eq(FriendRequestEntity::getTargetAccountId, owner.accountId())
				.eq(FriendRequestEntity::getStatus, "PENDING"));
		if (request == null) {
			throw new FriendRequestNotFoundException(requestId);
		}

		boolean exists = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelationEntity>()
				.eq(FriendRelationEntity::getOwnerAccountId, owner.accountId())
				.eq(FriendRelationEntity::getFriendAccountId, request.getRequesterAccountId())) > 0;
		if (exists) {
			request.setStatus("ACCEPTED");
			request.setRespondedAt(LocalDateTime.now());
			friendRequestMapper.updateById(request);
			throw new FriendAlreadyExistsException(request.getRequesterAccountId());
		}

		friendRelationMapper.insert(createRelation(owner.accountId(), request.getRequesterAccountId(), request.getNote()));
		friendRelationMapper.insert(createRelation(request.getRequesterAccountId(), owner.accountId(), request.getNote()));
		request.setStatus("ACCEPTED");
		request.setRespondedAt(LocalDateTime.now());
		friendRequestMapper.updateById(request);
		return new FriendRecord(request.getRequesterAccountId(), request.getRequesterDisplayName(), request.getNote());
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
		FriendRequestEntity request = friendRequestMapper.selectOne(new LambdaQueryWrapper<FriendRequestEntity>()
				.eq(FriendRequestEntity::getRequestId, requestId)
				.eq(FriendRequestEntity::getTargetAccountId, owner.accountId())
				.eq(FriendRequestEntity::getStatus, "PENDING"));
		if (request == null) {
			throw new FriendRequestNotFoundException(requestId);
		}
		request.setStatus(nextStatus);
		request.setRespondedAt(LocalDateTime.now());
		friendRequestMapper.updateById(request);
		return new FriendRequestRecord(
				request.getRequestId(),
				request.getRequesterAccountId(),
				request.getRequesterDisplayName(),
				"INCOMING",
				request.getStatus(),
				request.getNote()
		);
	}

	private FriendRelationEntity createRelation(String ownerAccountId, String friendAccountId, String note) {
		FriendRelationEntity entity = new FriendRelationEntity();
		entity.setOwnerAccountId(ownerAccountId);
		entity.setFriendAccountId(friendAccountId);
		entity.setNote(note);
		entity.setCreatedAt(LocalDateTime.now());
		return entity;
	}

	private String resolveDisplayName(String accountId) {
		AccountEntity accountEntity = accountMapper.selectOne(new LambdaQueryWrapper<AccountEntity>()
				.eq(AccountEntity::getAccountId, accountId));
		return accountEntity == null ? accountId : accountEntity.getDisplayName();
	}
}
