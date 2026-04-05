package com.kzzz3.argus.cortex.auth.web;

import com.kzzz3.argus.cortex.auth.application.AuthApplicationService;
import jakarta.validation.Valid;
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
}
