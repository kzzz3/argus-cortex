package com.kzzz3.argus.cortex.auth.domain;

import java.time.LocalDateTime;

public record RefreshSessionRecord(
		String sessionId,
		String accountId,
		String displayName,
		String refreshTokenHash,
		LocalDateTime expiresAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime revokedAt
) {
}
