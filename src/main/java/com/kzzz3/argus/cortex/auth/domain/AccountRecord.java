package com.kzzz3.argus.cortex.auth.domain;

public record AccountRecord(
		String accountId,
		String displayName,
		String passwordHash
) {
}
