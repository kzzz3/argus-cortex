package com.kzzz3.argus.cortex.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kzzz3.argus.cortex.auth.config.JwtSecretKeyProvider;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionRecord;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionStore;
import com.kzzz3.argus.cortex.auth.infrastructure.InMemoryAccountStore;
import com.kzzz3.argus.cortex.auth.infrastructure.InMemoryRefreshSessionStore;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TimeZone;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class AuthApplicationServiceTest {

	private static final String TEST_SECRET = "argus-stage1-dev-secret-key-please-change-1234567890";

	private AuthApplicationService authApplicationService;
	private AuthenticatedAccountResolver authenticatedAccountResolver;
	private InMemoryAccountStore accountStore;

	@BeforeEach
	void setUp() {
		accountStore = new InMemoryAccountStore();
		JwtSecretKeyProvider jwtSecretKeyProvider = new JwtSecretKeyProvider(TEST_SECRET);
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		authenticatedAccountResolver = new AuthenticatedAccountResolver(jwtDecoder, accountStore);
		authApplicationService = new AuthApplicationService(
				accountStore,
				new JwtTokenService(jwtEncoder, "PT168H", "PT168H", jwtSecretKeyProvider),
				new OpaqueRefreshTokenService("PT168H"),
				new InMemoryRefreshSessionStore(),
				authenticatedAccountResolver
		);
	}

	@Test
	void registerThenLoginReturnsServerSession() {
		AuthResult registerResult = authApplicationService.register(
				new RegisterCommand("Argus Tester", "tester", "secret123")
		);

		AuthResult loginResult = authApplicationService.login(
				new LoginCommand("tester", "secret123")
		);

		assertEquals("tester", registerResult.accountId());
		assertEquals("Argus Tester", registerResult.displayName());
		assertNotNull(registerResult.accessToken());
		assertNotNull(registerResult.refreshToken());
		assertEquals("tester", authenticatedAccountResolver.resolve(registerResult.accessToken()).accountId());
		assertEquals("tester", loginResult.accountId());
		assertEquals("tester", authenticatedAccountResolver.resolve(loginResult.accessToken()).accountId());
		assertNotNull(loginResult.refreshToken());
		assertEquals("Login succeeded. JWT bearer token issued.", loginResult.message());
	}

	@Test
	void refreshReturnsNewTokenPair() {
		AuthResult loginResult = authApplicationService.register(new RegisterCommand("Argus Tester", "tester-refresh", "secret123"));
		AuthResult refreshResult = authApplicationService.refresh(new RefreshTokenCommand(loginResult.refreshToken()));

		assertEquals("tester-refresh", refreshResult.accountId());
		assertNotNull(refreshResult.accessToken());
		assertNotNull(refreshResult.refreshToken());
	}

	@Test
	void duplicateRegisterThrowsConflict() {
		authApplicationService.register(new RegisterCommand("Argus Tester", "tester-2", "secret123"));

		assertThrows(
				RegistrationConflictException.class,
				() -> authApplicationService.register(new RegisterCommand("Argus Tester", "tester-2", "secret123"))
		);
	}

	@Test
	void loginWithWrongPasswordThrowsUnauthorized() {
		authApplicationService.register(new RegisterCommand("Argus Tester", "tester-3", "secret123"));

		assertThrows(
				InvalidCredentialsException.class,
				() -> authApplicationService.login(new LoginCommand("tester-3", "wrongpass"))
		);
	}

	@Test
	void refreshUsesUtcNowForSessionLookupInNonUtcDefaultTimezone() {
		TimeZone original = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));
		try {
			InMemoryAccountStore localAccountStore = new InMemoryAccountStore();
			JwtSecretKeyProvider jwtSecretKeyProvider = new JwtSecretKeyProvider(TEST_SECRET);
			JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
			JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
					.macAlgorithm(MacAlgorithm.HS256)
					.build();
			AuthenticatedAccountResolver resolver = new AuthenticatedAccountResolver(jwtDecoder, localAccountStore);
			OpaqueRefreshTokenService refreshTokenService = new OpaqueRefreshTokenService("PT168H");
			CapturingRefreshSessionStore refreshSessionStore = new CapturingRefreshSessionStore();
			AuthApplicationService service = new AuthApplicationService(
					localAccountStore,
					new JwtTokenService(jwtEncoder, "PT168H", "PT168H", jwtSecretKeyProvider),
					refreshTokenService,
					refreshSessionStore,
					resolver
			);

			AccountRecord account = localAccountStore.save(new AccountRecord(
					"tester-timezone",
					"Argus Tester",
					"secret123"
			));
			String rawRefreshToken = refreshTokenService.issueToken();
			refreshSessionStore.session = new RefreshSessionRecord(
					"session-1",
					account.accountId(),
					account.displayName(),
					refreshTokenService.hash(rawRefreshToken),
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5),
					LocalDateTime.now(ZoneOffset.UTC),
					LocalDateTime.now(ZoneOffset.UTC),
					null
			);

			AuthResult refreshResult = service.refresh(new RefreshTokenCommand(rawRefreshToken));

			assertEquals("tester-timezone", refreshResult.accountId());
			assertNotNull(refreshResult.accessToken());
			assertNotNull(refreshResult.refreshToken());
			assertTrue(Duration.between(refreshSessionStore.lookupNow, LocalDateTime.now(ZoneOffset.UTC)).abs().getSeconds() < 5);
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	void registerUsesUtcTimestampsForRefreshSessionLifecycle() {
		CapturingRefreshSessionStore refreshSessionStore = new CapturingRefreshSessionStore();
		authApplicationService = new AuthApplicationService(
				accountStore,
				new JwtTokenService(
						new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))),
						"PT168H",
						"PT168H",
						new JwtSecretKeyProvider(TEST_SECRET)
				),
				new OpaqueRefreshTokenService("PT168H"),
				refreshSessionStore,
				authenticatedAccountResolver
		);

		authApplicationService.register(new RegisterCommand("Argus Tester", "tester-utc", "secret123"));

		assertNotNull(refreshSessionStore.createdRecord);
		LocalDateTime derivedIssuedAt = refreshSessionStore.createdRecord.expiresAt().minusHours(168);
		assertTrue(Duration.between(refreshSessionStore.createdRecord.createdAt(), derivedIssuedAt).abs().getSeconds() < 5);
		assertTrue(Duration.between(refreshSessionStore.createdRecord.updatedAt(), derivedIssuedAt).abs().getSeconds() < 5);
	}

	private static class CapturingRefreshSessionStore implements RefreshSessionStore {
		private RefreshSessionRecord session;
		private RefreshSessionRecord createdRecord;
		private LocalDateTime lookupNow;

		@Override
		public void create(RefreshSessionRecord record) {
			this.createdRecord = record;
			this.session = record;
		}

		@Override
		public Optional<RefreshSessionRecord> findActiveByTokenHash(String tokenHash, LocalDateTime now) {
			this.lookupNow = now;
			if (session == null) {
				return Optional.empty();
			}
			if (!session.refreshTokenHash().equals(tokenHash)) {
				return Optional.empty();
			}
			if (session.revokedAt() != null || !session.expiresAt().isAfter(now)) {
				return Optional.empty();
			}
			return Optional.of(session);
		}

		@Override
		public RefreshSessionRecord rotate(String sessionId, String nextTokenHash, LocalDateTime nextExpiresAt, LocalDateTime rotatedAt) {
			session = new RefreshSessionRecord(
					session.sessionId(),
					session.accountId(),
					session.displayName(),
					nextTokenHash,
					nextExpiresAt,
					session.createdAt(),
					rotatedAt,
					null
			);
			return session;
		}

		@Override
		public void revoke(String sessionId, LocalDateTime revokedAt) {
			if (session == null) {
				return;
			}
			session = new RefreshSessionRecord(
					session.sessionId(),
					session.accountId(),
					session.displayName(),
					session.refreshTokenHash(),
					session.expiresAt(),
					session.createdAt(),
					session.updatedAt(),
					revokedAt
			);
		}
	}
}
