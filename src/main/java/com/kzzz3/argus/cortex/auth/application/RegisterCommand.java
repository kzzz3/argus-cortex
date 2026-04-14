package com.kzzz3.argus.cortex.auth.application;

public record RegisterCommand(
		String displayName,
		String account,
		String password
) {
}
