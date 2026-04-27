package com.kzzz3.argus.cortex.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OperationsConfigurationTest {

	@Test
	void runtimeConfigurationIncludesActuatorHealthReadinessBaseline() throws IOException {
		String pom = Files.readString(Path.of("pom.xml"));
		String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

		assertTrue(pom.contains("spring-boot-starter-actuator"), "Actuator should be part of the ops baseline.");
		assertTrue(applicationYaml.contains("management:"), "Management config should be explicit.");
		assertTrue(applicationYaml.contains("probes:"), "Health probes should be enabled for orchestration readiness/liveness.");
		assertTrue(applicationYaml.contains("exposure:"), "Exposed actuator endpoints should be explicitly bounded.");
	}

	@Test
	void dockerComposePinsInfrastructureImages() throws IOException {
		String dockerCompose = Files.readString(Path.of("docker-compose.yml"));

		assertTrue(!dockerCompose.contains(":latest"), "Local infrastructure images should be version-pinned for reproducibility.");
	}
}
