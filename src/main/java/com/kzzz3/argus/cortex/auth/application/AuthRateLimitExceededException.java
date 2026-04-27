package com.kzzz3.argus.cortex.auth.application;

public class AuthRateLimitExceededException extends RuntimeException {

	public AuthRateLimitExceededException() {
		super("Too many authentication attempts. Please retry later.");
	}
}
