package com.kzzz3.argus.cortex.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

	private final int maxAttempts;
	private final Duration window;
	private final Clock clock;
	private final Map<String, AttemptWindow> attemptsByKey = new ConcurrentHashMap<>();

	@Autowired
	public AuthRateLimiter(
			@Value("${argus.auth.rate-limit.max-attempts:20}") int maxAttempts,
			@Value("${argus.auth.rate-limit.window:PT1M}") String window
	) {
		this(maxAttempts, Duration.parse(window), Clock.systemUTC());
	}

	AuthRateLimiter(int maxAttempts, Duration window, Clock clock) {
		if (maxAttempts <= 0) {
			throw new IllegalArgumentException("maxAttempts must be positive.");
		}
		if (window == null || window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be positive.");
		}
		this.maxAttempts = maxAttempts;
		this.window = window;
		this.clock = clock;
	}

	public void check(String key) {
		String normalizedKey = key == null || key.isBlank() ? "unknown" : key.trim();
		Instant now = clock.instant();
		AttemptWindow attemptWindow = attemptsByKey.computeIfAbsent(
				normalizedKey,
				ignored -> new AttemptWindow(now.plus(window))
		);
		synchronized (attemptWindow) {
			if (!now.isBefore(attemptWindow.expiresAt)) {
				attemptWindow.expiresAt = now.plus(window);
				attemptWindow.count = 0;
			}
			if (attemptWindow.count >= maxAttempts) {
				throw new AuthRateLimitExceededException();
			}
			attemptWindow.count++;
		}
	}

	private static final class AttemptWindow {
		private Instant expiresAt;
		private int count;

		private AttemptWindow(Instant expiresAt) {
			this.expiresAt = expiresAt;
		}
	}
}
