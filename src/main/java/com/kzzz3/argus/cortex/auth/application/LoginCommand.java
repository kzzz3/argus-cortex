package com.kzzz3.argus.cortex.auth.application;

public record LoginCommand(
		String account,
		String password
) {
}
