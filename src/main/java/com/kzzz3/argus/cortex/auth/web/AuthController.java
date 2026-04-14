package com.kzzz3.argus.cortex.auth.web;

import com.kzzz3.argus.cortex.auth.application.AuthApplicationService;
import com.kzzz3.argus.cortex.auth.application.LoginCommand;
import com.kzzz3.argus.cortex.auth.application.RegisterCommand;
import com.kzzz3.argus.cortex.shared.web.BearerTokenExtractor;
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
		return AuthSuccessResponse.from(authApplicationService.register(
				new RegisterCommand(request.displayName(), request.account(), request.password())
		));
	}

	@PostMapping("/login")
	public AuthSuccessResponse login(@Valid @RequestBody LoginRequest request) {
		return AuthSuccessResponse.from(authApplicationService.login(
				new LoginCommand(request.account(), request.password())
		));
	}

	@GetMapping("/session/me")
	public AuthSuccessResponse restoreSession(@RequestHeader("Authorization") String authorizationHeader) {
		return AuthSuccessResponse.from(
				authApplicationService.restoreSession(BearerTokenExtractor.extract(authorizationHeader))
		);
	}
}
