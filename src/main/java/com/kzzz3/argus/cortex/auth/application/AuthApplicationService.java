package com.kzzz3.argus.cortex.auth.application;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionRecord;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionStore;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthApplicationService {

	private final AccountStore accountStore;
	private final JwtTokenService jwtTokenService;
	private final OpaqueRefreshTokenService opaqueRefreshTokenService;
	private final RefreshSessionStore refreshSessionStore;
	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final PasswordEncoder passwordEncoder;

	public AuthApplicationService(
			AccountStore accountStore,
			JwtTokenService jwtTokenService,
			OpaqueRefreshTokenService opaqueRefreshTokenService,
			RefreshSessionStore refreshSessionStore,
			AuthenticatedAccountResolver authenticatedAccountResolver,
			PasswordEncoder passwordEncoder
	) {
		this.accountStore = accountStore;
		this.jwtTokenService = jwtTokenService;
		this.opaqueRefreshTokenService = opaqueRefreshTokenService;
		this.refreshSessionStore = refreshSessionStore;
		this.authenticatedAccountResolver = authenticatedAccountResolver;
		this.passwordEncoder = passwordEncoder;
	}

	public AuthResult register(RegisterCommand request) {
		String normalizedAccount = normalizeAccount(request.account());
		if (accountStore.exists(normalizedAccount)) {
			throw new RegistrationConflictException(normalizedAccount);
		}

		AccountRecord registeredAccount = accountStore.save(
				new AccountRecord(
						normalizedAccount,
						request.displayName().trim(),
						passwordEncoder.encode(request.password())
				)
		);

		return new AuthResult(
				registeredAccount.accountId(),
				registeredAccount.displayName(),
				jwtTokenService.issueAccessToken(registeredAccount),
				issueRefreshSession(registeredAccount),
				"Registration succeeded. JWT bearer token issued."
		);
	}

	public AuthResult login(LoginCommand request) {
		String normalizedAccount = normalizeAccount(request.account());
		AccountRecord accountRecord = accountStore.findByAccountId(normalizedAccount)
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), accountRecord.passwordHash())) {
			throw new InvalidCredentialsException();
		}

		return new AuthResult(
				accountRecord.accountId(),
				accountRecord.displayName(),
				jwtTokenService.issueAccessToken(accountRecord),
				issueRefreshSession(accountRecord),
				"Login succeeded. JWT bearer token issued."
		);
	}

	public AuthResult restoreSession(Jwt jwt) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolve(jwt);

		return new AuthResult(
				accountRecord.accountId(),
				accountRecord.displayName(),
				jwt.getTokenValue(),
				null,
				"Session restored from JWT bearer token."
		);
	}

	public AuthResult refresh(RefreshTokenCommand request) {
		String rawRefreshToken = request.refreshToken().trim();
		String tokenHash = opaqueRefreshTokenService.hash(rawRefreshToken);
		LocalDateTime now = utcNow();
		RefreshSessionRecord session = refreshSessionStore.findActiveByTokenHash(tokenHash, now)
				.orElseThrow(InvalidCredentialsException::new);
		AccountRecord accountRecord = accountStore.findByAccountId(session.accountId())
				.orElseThrow(InvalidCredentialsException::new);
		String nextRefreshToken = opaqueRefreshTokenService.issueToken();
		refreshSessionStore.rotate(
				session.sessionId(),
				opaqueRefreshTokenService.hash(nextRefreshToken),
				opaqueRefreshTokenService.expiresAt(),
				now
		);
		return new AuthResult(
				accountRecord.accountId(),
				accountRecord.displayName(),
				jwtTokenService.issueAccessToken(accountRecord),
				nextRefreshToken,
				"Access token refreshed from refresh token."
		);
	}

	private String issueRefreshSession(AccountRecord accountRecord) {
		String refreshToken = opaqueRefreshTokenService.issueToken();
		LocalDateTime now = utcNow();
		refreshSessionStore.create(new RefreshSessionRecord(
				opaqueRefreshTokenService.newSessionId(),
				accountRecord.accountId(),
				accountRecord.displayName(),
				opaqueRefreshTokenService.hash(refreshToken),
				opaqueRefreshTokenService.expiresAt(),
				now,
				now,
				null
		));
		return refreshToken;
	}

	private LocalDateTime utcNow() {
		return LocalDateTime.now(ZoneOffset.UTC);
	}

	private String normalizeAccount(String account) {
		return account.trim().toLowerCase();
	}
}
