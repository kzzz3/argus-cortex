package com.kzzz3.argus.cortex.auth.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JwtApplicationConfigurationTest {

	@Test
	void runtimeConfigurationRequiresExternalJwtSecretAndShortAccessTokenTtl() throws IOException {
		String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

		assertTrue(
				applicationYaml.contains(
						"secret: ${ARGUS_JWT_SECRET:}"
				),
				"Runtime JWT secret must come from ARGUS_JWT_SECRET and fail fast when omitted"
		);
		assertTrue(
				applicationYaml.contains("ttl: ${ARGUS_JWT_TTL:PT2H}"),
				"Access tokens should default to a short-lived production-safe TTL"
		);
		assertTrue(
				applicationYaml.contains("refresh-ttl: ${ARGUS_JWT_REFRESH_TTL:PT168H}"),
				"Refresh tokens can keep a longer explicit TTL boundary"
		);
		assertFalse(
				applicationYaml.contains("argus-cortex-local-dev-jwt-secret-please-overwrite-123456"),
				"Known fallback signing secrets must not be valid runtime defaults"
		);
	}
}
