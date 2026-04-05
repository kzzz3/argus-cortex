package com.kzzz3.argus.cortex.auth.domain;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid account or password");
	}
}
