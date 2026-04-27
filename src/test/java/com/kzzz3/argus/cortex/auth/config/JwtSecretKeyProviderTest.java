package com.kzzz3.argus.cortex.auth.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JwtSecretKeyProviderTest {

	@Test
	void blankSecretIsRejected() {
		assertThrows(IllegalStateException.class, () -> new JwtSecretKeyProvider("   "));
	}

	@Test
	void exampleSecretIsRejected() {
		assertThrows(
				IllegalStateException.class,
				() -> new JwtSecretKeyProvider("REPLACE_WITH_A_LONG_RANDOM_JWT_SECRET")
		);
		assertThrows(
				IllegalStateException.class,
				() -> new JwtSecretKeyProvider("replace_with_a_private_jwt_secret_min_32_chars")
		);
		assertThrows(
				IllegalStateException.class,
				() -> new JwtSecretKeyProvider("argus-cortex-local-dev-jwt-secret-please-overwrite-123456")
		);
	}

	@Test
	void shortSecretIsRejected() {
		assertThrows(IllegalStateException.class, () -> new JwtSecretKeyProvider("too-short-secret"));
	}

	@Test
	void validSecretExposesNormalizedValueAndSecretKey() {
		String secret = "argus-stage1-valid-secret-1234567890";
		JwtSecretKeyProvider provider = new JwtSecretKeyProvider("  " + secret + "  ");

		assertEquals(secret, provider.value());
		assertArrayEquals(secret.getBytes(StandardCharsets.UTF_8), provider.secretKey().getEncoded());
	}
}
