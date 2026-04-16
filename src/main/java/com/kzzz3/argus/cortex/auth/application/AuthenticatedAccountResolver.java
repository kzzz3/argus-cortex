package com.kzzz3.argus.cortex.auth.application;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedAccountResolver {

	private final JwtDecoder jwtDecoder;
	private final AccountStore accountStore;

	public AuthenticatedAccountResolver(JwtDecoder jwtDecoder, AccountStore accountStore) {
		this.jwtDecoder = jwtDecoder;
		this.accountStore = accountStore;
	}

	public AccountRecord resolve(String accessToken) {
		try {
			Jwt jwt = jwtDecoder.decode(accessToken);
			return resolve(jwt);
		} catch (JwtException exception) {
			throw new InvalidCredentialsException();
		}
	}

	public AccountRecord resolve(Jwt jwt) {
		String accountId = jwt.getSubject();
		return accountStore.findByAccountId(accountId)
				.orElseThrow(InvalidCredentialsException::new);
	}

	public AccountRecord resolveCurrent() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new InvalidCredentialsException();
		}
		if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
			return resolve(jwtAuthenticationToken.getToken());
		}
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			return resolve(jwt);
		}
		String principalName = authentication.getName();
		if (principalName == null || principalName.isBlank() || "anonymousUser".equals(principalName)) {
			throw new InvalidCredentialsException();
		}
		return accountStore.findByAccountId(principalName)
				.orElseThrow(InvalidCredentialsException::new);
	}
}
