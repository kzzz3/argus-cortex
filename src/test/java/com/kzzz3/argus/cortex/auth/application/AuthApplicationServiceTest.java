package com.kzzz3.argus.cortex.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.infrastructure.InMemoryAccountStore;
import com.kzzz3.argus.cortex.auth.infrastructure.UuidAccessTokenIssuer;
import com.kzzz3.argus.cortex.auth.web.LoginRequest;
import com.kzzz3.argus.cortex.auth.web.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthApplicationServiceTest {

	private AuthApplicationService authApplicationService;

	@BeforeEach
	void setUp() {
		authApplicationService = new AuthApplicationService(
				new InMemoryAccountStore(),
				new UuidAccessTokenIssuer()
		);
	}

	@Test
	void registerThenLoginReturnsServerSession() {
		AuthResult registerResult = authApplicationService.register(
				new RegisterRequest("Argus Tester", "tester", "secret123")
		);

		AuthResult loginResult = authApplicationService.login(
				new LoginRequest("tester", "secret123")
		);

		assertEquals("tester", registerResult.accountId());
		assertEquals("Argus Tester", registerResult.displayName());
		assertNotNull(registerResult.accessToken());
		assertEquals("tester", loginResult.accountId());
		assertEquals("Login succeeded. Stage-1 server session issued.", loginResult.message());
	}

	@Test
	void duplicateRegisterThrowsConflict() {
		authApplicationService.register(new RegisterRequest("Argus Tester", "tester-2", "secret123"));

		assertThrows(
				RegistrationConflictException.class,
				() -> authApplicationService.register(new RegisterRequest("Argus Tester", "tester-2", "secret123"))
		);
	}

	@Test
	void loginWithWrongPasswordThrowsUnauthorized() {
		authApplicationService.register(new RegisterRequest("Argus Tester", "tester-3", "secret123"));

		assertThrows(
				InvalidCredentialsException.class,
				() -> authApplicationService.login(new LoginRequest("tester-3", "wrongpass"))
		);
	}
}
