package ai.devpath.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import ai.devpath.gateway.GatewayApplication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-3: JWT_SECRET 32바이트 미만이면 컨텍스트 로드 실패(IllegalStateException) 검증.
 * @SpringBootTest 전체 컨텍스트를 짧은 secret으로 띄우면 BeanCreationException이 발생해야 한다.
 */
class GatewaySecurityConfigShortSecretTest {

	@Test
	void shortJwtSecretRejectedWithIllegalStateException() {
		// SpringApplication을 직접 실행해 짧은 secret 주입 → 컨텍스트 로드 실패 검증
		Exception ex = assertThrows(Exception.class, () -> {
			ConfigurableApplicationContext ctx = new SpringApplication(GatewayApplication.class)
				.run("--server.port=0",
					"--spring.profiles.active=test",
					"--JWT_SECRET=short");
			ctx.close();
		});
		// cause 체인을 순회해 IllegalStateException + 메시지 검증 (I-1: 길이 검증이 실패 원인임을 단언)
		StringBuilder msgs = new StringBuilder();
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t.getMessage() != null) {
				msgs.append(t.getMessage()).append(" | ");
			}
		}
		String causeChain = msgs.toString();
		assertTrue(causeChain.contains("JWT_SECRET must be >= 32 bytes"),
			"cause 체인에 JWT_SECRET 길이 검증 메시지가 없음. 실제 메시지: " + causeChain);
	}
}
