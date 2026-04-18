package com.kzzz3.argus.cortex.auth.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretKeyProvider {

	private static final int MIN_SECRET_LENGTH = 32;
	private static final String EXAMPLE_SECRET = "REPLACE_WITH_A_LONG_RANDOM_JWT_SECRET";
	private static final String LEGACY_EXAMPLE_SECRET = "replace_with_a_private_jwt_secret_min_32_chars";

	private final String secret;
	private final SecretKey secretKey;

	public JwtSecretKeyProvider(@Value("${argus.auth.jwt.secret}") String secret) {
		String normalizedSecret = secret == null ? "" : secret.trim();
		if (normalizedSecret.isBlank()) {
			throw new IllegalStateException("argus.auth.jwt.secret must be configured.");
		}
		if (EXAMPLE_SECRET.equals(normalizedSecret) || LEGACY_EXAMPLE_SECRET.equals(normalizedSecret)) {
			throw new IllegalStateException("argus.auth.jwt.secret must be replaced with a real secret.");
		}
		if (normalizedSecret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException("argus.auth.jwt.secret must be at least 32 characters.");
		}
		this.secret = normalizedSecret;
		this.secretKey = new SecretKeySpec(normalizedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public String value() {
		return secret;
	}

	public SecretKey secretKey() {
		return secretKey;
	}
}
