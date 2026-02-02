package com.pms.pms

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.flyway.enabled=false"])
class PmsApplicationTests {

	@Test
	fun contextLoads() {
	}

}
