package com.kzzz3.argus.cortex.auth.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JwtApplicationConfigurationTest {

	@Test
	void localConfigurationKeepsJwtEnvOverrideAndRunnableFallback() throws IOException {
		String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

		assertTrue(
				applicationYaml.contains(
						"secret: ${ARGUS_JWT_SECRET:argus-cortex-local-dev-jwt-secret-please-overwrite-123456}"
				),
				"JWT secret config should keep ARGUS_JWT_SECRET override with a local spring-boot:run fallback"
		);
	}
}
