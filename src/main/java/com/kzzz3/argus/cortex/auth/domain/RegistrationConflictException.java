package com.kzzz3.argus.cortex.auth.domain;

public class RegistrationConflictException extends RuntimeException {

	public RegistrationConflictException(String accountId) {
		super("Account already exists: " + accountId);
	}
}
