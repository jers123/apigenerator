package org.jers.apigenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest
class ApigeneratorApplicationTests {

	@TempDir
	static Path dataDir;

	@DynamicPropertySource
	static void dataDirInTemporaryFolder(final DynamicPropertyRegistry registry) {
		registry.add("app.data-dir", () -> dataDir.toString());
	}

	@Test
	void contextLoads() {
	}
}