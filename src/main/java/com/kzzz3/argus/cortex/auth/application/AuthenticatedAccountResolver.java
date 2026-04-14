package com.kzzz3.argus.cortex.auth.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedAccountResolver {

	private final AccessTokenStore accessTokenStore;

	public AuthenticatedAccountResolver(AccessTokenStore accessTokenStore) {
		this.accessTokenStore = accessTokenStore;
	}

	public AccountRecord resolve(String accessToken) {
		return accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
	}
}
