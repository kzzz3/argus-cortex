package com.kzzz3.argus.cortex.friend.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AccountEntity;
import com.kzzz3.argus.cortex.auth.infrastructure.mapper.AccountMapper;
import com.kzzz3.argus.cortex.friend.domain.FriendAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendRecord;
import com.kzzz3.argus.cortex.friend.domain.FriendStore;
import com.kzzz3.argus.cortex.friend.infrastructure.entity.FriendRelationEntity;
import com.kzzz3.argus.cortex.friend.infrastructure.mapper.FriendRelationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisFriendStore implements FriendStore {

	private final FriendRelationMapper friendRelationMapper;
	private final AccountMapper accountMapper;

	public MybatisFriendStore(FriendRelationMapper friendRelationMapper, AccountMapper accountMapper) {
		this.friendRelationMapper = friendRelationMapper;
		this.accountMapper = accountMapper;
	}

	@Override
	public List<FriendRecord> listFriends(AccountRecord owner) {
		seedDefaultFriend(owner);
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
	public FriendRecord addFriend(AccountRecord owner, AccountRecord friend) {
		boolean exists = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelationEntity>()
				.eq(FriendRelationEntity::getOwnerAccountId, owner.accountId())
				.eq(FriendRelationEntity::getFriendAccountId, friend.accountId())) > 0;
		if (exists) {
			throw new FriendAlreadyExistsException(friend.accountId());
		}

		friendRelationMapper.insert(createRelation(owner.accountId(), friend.accountId(), "Added from remote contact flow"));
		friendRelationMapper.insert(createRelation(friend.accountId(), owner.accountId(), "Added from remote contact flow"));
		return new FriendRecord(friend.accountId(), friend.displayName(), "Added from remote contact flow");
	}

	private void seedDefaultFriend(AccountRecord owner) {
		if (owner.accountId().equals("zhangsan")) {
			return;
		}
		long count = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelationEntity>()
				.eq(FriendRelationEntity::getOwnerAccountId, owner.accountId()));
		if (count == 0L) {
			friendRelationMapper.insert(createRelation(owner.accountId(), "zhangsan", "Default remote contact"));
		}
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
