package com.kzzz3.argus.cortex.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpaqueRefreshTokenService {
	private final Duration refreshTokenTtl;
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

	public OpaqueRefreshTokenService(@Value("${argus.auth.jwt.refresh-ttl:PT168H}") String refreshTokenTtl) {
		this.refreshTokenTtl = Duration.parse(refreshTokenTtl);
	}

	public String issueToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 unavailable", exception);
		}
	}

	public String newSessionId() {
		return UUID.randomUUID().toString();
	}

	public LocalDateTime expiresAt() {
		return LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenTtl);
	}
}
