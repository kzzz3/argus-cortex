package com.kzzz3.argus.cortex.auth.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AccountEntity;
import com.kzzz3.argus.cortex.auth.infrastructure.mapper.AccountMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisAccountStore implements AccountStore {

	private final AccountMapper accountMapper;

	public MybatisAccountStore(AccountMapper accountMapper) {
		this.accountMapper = accountMapper;
	}

	@Override
	public boolean exists(String accountId) {
		return accountMapper.selectCount(new LambdaQueryWrapper<AccountEntity>()
				.eq(AccountEntity::getAccountId, accountId)) > 0;
	}

	@Override
	public AccountRecord save(AccountRecord accountRecord) {
		AccountEntity entity = new AccountEntity();
		entity.setAccountId(accountRecord.accountId());
		entity.setDisplayName(accountRecord.displayName());
		entity.setPassword(accountRecord.password());
		entity.setCreatedAt(LocalDateTime.now());
		try {
			accountMapper.insert(entity);
		} catch (DuplicateKeyException duplicateKeyException) {
			throw new RegistrationConflictException(accountRecord.accountId());
		}
		return accountRecord;
	}

	@Override
	public Optional<AccountRecord> findByAccountId(String accountId) {
		AccountEntity entity = accountMapper.selectOne(new LambdaQueryWrapper<AccountEntity>()
				.eq(AccountEntity::getAccountId, accountId));
		return Optional.ofNullable(entity)
				.map(found -> new AccountRecord(found.getAccountId(), found.getDisplayName(), found.getPassword()));
	}
}
