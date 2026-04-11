package com.kzzz3.argus.cortex.auth.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class UuidAccessTokenIssuer implements AccessTokenStore {

	private final Map<String, AccountRecord> sessionByToken = new ConcurrentHashMap<>();

	@Override
	public String issue(AccountRecord accountRecord) {
		String accessToken = "argus-" + UUID.randomUUID();
		sessionByToken.put(accessToken, accountRecord);
		return accessToken;
	}

	@Override
	public Optional<AccountRecord> findByToken(String accessToken) {
		return Optional.ofNullable(sessionByToken.get(accessToken));
	}
}
