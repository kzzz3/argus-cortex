package com.kzzz3.argus.cortex.auth.web;

import com.kzzz3.argus.cortex.auth.application.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthApplicationService authApplicationService;

	public AuthController(AuthApplicationService authApplicationService) {
		this.authApplicationService = authApplicationService;
	}

	@PostMapping("/register")
	public AuthSuccessResponse register(@Valid @RequestBody RegisterRequest request) {
		return AuthSuccessResponse.from(authApplicationService.register(request));
	}

	@PostMapping("/login")
	public AuthSuccessResponse login(@Valid @RequestBody LoginRequest request) {
		return AuthSuccessResponse.from(authApplicationService.login(request));
	}

	@GetMapping("/session/me")
	public AuthSuccessResponse restoreSession(@RequestHeader("Authorization") String authorizationHeader) {
		return AuthSuccessResponse.from(
				authApplicationService.restoreSession(extractBearerToken(authorizationHeader))
		);
	}

	private String extractBearerToken(String authorizationHeader) {
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
