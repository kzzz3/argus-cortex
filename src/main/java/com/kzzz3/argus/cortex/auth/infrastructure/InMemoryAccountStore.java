package com.kzzz3.argus.cortex.auth.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryAccountStore implements AccountStore {

	private final ConcurrentMap<String, AccountRecord> accountsById = new ConcurrentHashMap<>();

	@Override
	public boolean exists(String accountId) {
		return accountsById.containsKey(accountId);
	}

	@Override
	public AccountRecord save(AccountRecord accountRecord) {
		AccountRecord existing = accountsById.putIfAbsent(accountRecord.accountId(), accountRecord);
		if (existing != null) {
			throw new RegistrationConflictException(accountRecord.accountId());
		}
		return accountRecord;
	}

	@Override
	public Optional<AccountRecord> findByAccountId(String accountId) {
		return Optional.ofNullable(accountsById.get(accountId));
	}
}
