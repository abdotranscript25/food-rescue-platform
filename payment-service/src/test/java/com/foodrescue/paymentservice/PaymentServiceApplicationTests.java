package com.foodrescue.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // ⬅️ LIGNE CRUCIALE : force l'utilisation de H2 en mémoire
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
		// Ce test vérifie juste que le contexte Spring démarre correctement
	}
}