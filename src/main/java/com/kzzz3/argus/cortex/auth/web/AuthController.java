package com.kzzz3.argus.cortex.auth.web;

import com.kzzz3.argus.cortex.auth.application.AuthApplicationService;
import com.kzzz3.argus.cortex.auth.application.AuthRateLimiter;
import com.kzzz3.argus.cortex.auth.application.LoginCommand;
import com.kzzz3.argus.cortex.auth.application.RefreshTokenCommand;
import com.kzzz3.argus.cortex.auth.application.RegisterCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthApplicationService authApplicationService;
	private final AuthRateLimiter authRateLimiter;

	public AuthController(AuthApplicationService authApplicationService, AuthRateLimiter authRateLimiter) {
		this.authApplicationService = authApplicationService;
		this.authRateLimiter = authRateLimiter;
	}

	@PostMapping("/register")
	public AuthSuccessResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
		authRateLimiter.check(rateLimitKey("register", servletRequest, request.account()));
		return AuthSuccessResponse.from(authApplicationService.register(
				new RegisterCommand(request.displayName(), request.account(), request.password())
		));
	}

	@PostMapping("/login")
	public AuthSuccessResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		authRateLimiter.check(rateLimitKey("login", servletRequest, request.account()));
		return AuthSuccessResponse.from(authApplicationService.login(
				new LoginCommand(request.account(), request.password())
		));
	}

	@PostMapping("/refresh")
	public AuthSuccessResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
		authRateLimiter.check(rateLimitKey("refresh", servletRequest, "refresh"));
		return AuthSuccessResponse.from(authApplicationService.refresh(new RefreshTokenCommand(request.refreshToken())));
	}

	@GetMapping("/session/me")
	public AuthSuccessResponse restoreSession(JwtAuthenticationToken authentication) {
		return AuthSuccessResponse.from(
				authApplicationService.restoreSession(authentication.getToken())
		);
	}

	private String rateLimitKey(String action, HttpServletRequest request, String account) {
		String remoteAddress = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
		String normalizedAccount = account == null || account.isBlank() ? "unknown" : account.trim().toLowerCase();
		return action + ":" + remoteAddress + ":" + normalizedAccount;
	}
}
