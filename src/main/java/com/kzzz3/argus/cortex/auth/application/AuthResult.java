package com.kzzz3.argus.cortex.auth.application;

public record AuthResult(
		String accountId,
		String displayName,
		String accessToken,
		String refreshToken,
		String message
) {
}
