package com.kzzz3.argus.cortex.shared.web;

public final class BearerTokenExtractor {

	private BearerTokenExtractor() {
	}

	public static String extract(String authorizationHeader) {
		if (authorizationHeader == null) {
			throw new IllegalArgumentException("Missing Authorization header.");
		}

		String prefix = "Bearer ";
		if (!authorizationHeader.startsWith(prefix)) {
			throw new IllegalArgumentException("Authorization header must use Bearer token.");
		}

		return authorizationHeader.substring(prefix.length()).trim();
	}
}
