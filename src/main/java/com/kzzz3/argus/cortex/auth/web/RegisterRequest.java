package com.kzzz3.argus.cortex.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Display name is required")
		String displayName,
		@NotBlank(message = "Account is required")
		@Size(min = 4, message = "Account must be at least 4 characters")
		String account,
		@NotBlank(message = "Password is required")
		@Size(min = 6, message = "Password must be at least 6 characters")
		String password
) {
}
