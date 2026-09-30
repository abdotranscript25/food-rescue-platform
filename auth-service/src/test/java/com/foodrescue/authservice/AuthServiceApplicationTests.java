package com.foodrescue.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
		// Vérifie simplement que le contexte Spring démarre correctement
	}
}