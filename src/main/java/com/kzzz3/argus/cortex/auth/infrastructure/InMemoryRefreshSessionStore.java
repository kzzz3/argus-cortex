package com.kzzz3.argus.cortex.auth.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.RefreshSessionRecord;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionStore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryRefreshSessionStore implements RefreshSessionStore {

	private final Map<String, RefreshSessionRecord> sessionsById = new ConcurrentHashMap<>();

	@Override
	public void create(RefreshSessionRecord record) {
		sessionsById.put(record.sessionId(), record);
	}

	@Override
	public Optional<RefreshSessionRecord> findActiveByTokenHash(String tokenHash, LocalDateTime now) {
		return sessionsById.values().stream()
				.filter(record -> record.refreshTokenHash().equals(tokenHash))
				.filter(record -> record.revokedAt() == null)
				.filter(record -> record.expiresAt().isAfter(now))
				.findFirst();
	}

	@Override
	public RefreshSessionRecord rotate(String sessionId, String nextTokenHash, LocalDateTime nextExpiresAt, LocalDateTime rotatedAt) {
		RefreshSessionRecord current = sessionsById.get(sessionId);
		RefreshSessionRecord rotated = new RefreshSessionRecord(
				current.sessionId(),
				current.accountId(),
				current.displayName(),
				nextTokenHash,
				nextExpiresAt,
				current.createdAt(),
				rotatedAt,
				null
		);
		sessionsById.put(sessionId, rotated);
		return rotated;
	}

	@Override
	public void revoke(String sessionId, LocalDateTime revokedAt) {
		RefreshSessionRecord current = sessionsById.get(sessionId);
		if (current == null) {
			return;
		}
		sessionsById.put(sessionId, new RefreshSessionRecord(
				current.sessionId(),
				current.accountId(),
				current.displayName(),
				current.refreshTokenHash(),
				current.expiresAt(),
				current.createdAt(),
				current.updatedAt(),
				revokedAt
		));
	}
}
