package com.kzzz3.argus.cortex.auth.web;

import com.kzzz3.argus.cortex.auth.application.AuthResult;

public record AuthSuccessResponse(
		String accountId,
		String displayName,
		String accessToken,
		String refreshToken,
		String message
) {
	public static AuthSuccessResponse from(AuthResult result) {
		return new AuthSuccessResponse(
				result.accountId(),
				result.displayName(),
				result.accessToken(),
				result.refreshToken(),
				result.message()
		);
	}
}
