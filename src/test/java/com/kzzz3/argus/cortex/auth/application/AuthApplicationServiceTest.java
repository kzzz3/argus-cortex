package com.kzzz3.argus.cortex.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.infrastructure.InMemoryAccountStore;
import com.kzzz3.argus.cortex.auth.infrastructure.InMemoryRefreshSessionStore;
import java.nio.charset.StandardCharsets;
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

	@BeforeEach
	void setUp() {
		InMemoryAccountStore accountStore = new InMemoryAccountStore();
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		authenticatedAccountResolver = new AuthenticatedAccountResolver(jwtDecoder, accountStore);
		authApplicationService = new AuthApplicationService(
				accountStore,
				new JwtTokenService(jwtEncoder, "PT168H", "PT168H", TEST_SECRET),
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
}
