package com.kzzz3.argus.cortex.auth.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshSessionStore {
	void create(RefreshSessionRecord record);

	Optional<RefreshSessionRecord> findActiveByTokenHash(String tokenHash, LocalDateTime now);

	RefreshSessionRecord rotate(String sessionId, String nextTokenHash, LocalDateTime nextExpiresAt, LocalDateTime rotatedAt);

	void revoke(String sessionId, LocalDateTime revokedAt);
}
