package com.kzzz3.argus.cortex.auth.domain;

import java.util.Optional;

public interface AccessTokenStore {

	String issue(AccountRecord accountRecord);

	Optional<AccountRecord> findByToken(String accessToken);
}
