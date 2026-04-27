package com.kzzz3.argus.cortex.auth.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

	@Test
	void check_allowsRequestsWithinWindowAndRejectsExcess() {
		AuthRateLimiter limiter = new AuthRateLimiter(2, Duration.ofMinutes(1), Clock.fixed(Instant.parse("2026-04-27T00:00:00Z"), ZoneOffset.UTC));

		assertDoesNotThrow(() -> limiter.check("login:127.0.0.1"));
		assertDoesNotThrow(() -> limiter.check("login:127.0.0.1"));
		assertThrows(AuthRateLimitExceededException.class, () -> limiter.check("login:127.0.0.1"));
	}
}
