package com.kzzz3.argus.cortex.auth.domain;

import java.util.Optional;

public interface AccountStore {

	boolean exists(String accountId);

	AccountRecord save(AccountRecord accountRecord);

	Optional<AccountRecord> findByAccountId(String accountId);
}
