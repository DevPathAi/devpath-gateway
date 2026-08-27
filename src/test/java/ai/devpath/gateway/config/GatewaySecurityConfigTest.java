package ai.devpath.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewaySecurityConfigTest {

	@LocalServerPort
	int port;

	WebTestClient web;

	@BeforeEach
	void setUp() {
		web = WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.build();
	}

	@Test
	void protectedRouteWithoutTokenIs401() {
		web.get().uri("/users/me").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void publicAuthRefreshIsNot401() {
		// 다운스트림 미가동 시 라우팅 실패(5xx/404)일 수 있으나 보안 401은 아니어야 한다.
		web.post().uri("/auth/refresh").exchange()
			.expectStatus().value(s -> org.junit.jupiter.api.Assertions.assertNotEquals(401, s));
	}

	@Test
	void runBoundReleaseBrowserEndpointIsNotRejectedAsJwtProtected() {
		web.get().uri("/v1/release/browser/analytics-permission")
			.header("X-Candidate-Spec-Sha256", "a".repeat(64))
			.header("X-Release-Run-Key", "A".repeat(22))
			.exchange()
			.expectStatus().value(s -> org.junit.jupiter.api.Assertions.assertNotEquals(401, s));
	}

	// P1-6: CORS preflight — 허용 origin에서 OPTIONS 요청 시 allow-credentials: true 반환
	@Test
	void corsPreflightAllowedOriginReturnsAllowCredentials() {
		web.options().uri("/users/me")
			.header("Origin", "http://localhost:5173")
			.header("Access-Control-Request-Method", "GET")
			.header("Access-Control-Request-Headers", "Authorization")
			.exchange()
			.expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
			.expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
	}

	@Test
	void corsPreflightAllowsReleaseBindingHeaders() {
		web.options().uri("/v1/release/browser/analytics-events")
			.header("Origin", "http://localhost:5173")
			.header("Access-Control-Request-Method", "POST")
			.header(
				"Access-Control-Request-Headers",
				"Content-Type,X-Candidate-Spec-Sha256,X-Release-Run-Key")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().value("Access-Control-Allow-Headers", value -> {
				org.junit.jupiter.api.Assertions.assertTrue(
					value.toLowerCase().contains("x-candidate-spec-sha256"));
				org.junit.jupiter.api.Assertions.assertTrue(
					value.toLowerCase().contains("x-release-run-key"));
			});
	}

	@Test
	void corsPreflightAllowsSandboxEventVersionHeader() {
		web.options().uri("/sandbox/run")
			.header("Origin", "http://localhost:5173")
			.header("Access-Control-Request-Method", "POST")
			.header(
				"Access-Control-Request-Headers",
				"Content-Type,X-Candidate-Spec-Sha256,X-Release-Run-Key,X-Sandbox-Event-Version")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().value("Access-Control-Allow-Headers", value ->
				org.junit.jupiter.api.Assertions.assertTrue(
					value.toLowerCase().contains("x-sandbox-event-version")));
	}

	// P1-6: CORS preflight — 비허용 origin은 allow-credentials 헤더 없음
	@Test
	void corsPreflightDisallowedOriginNoAllowCredentials() {
		web.options().uri("/users/me")
			.header("Origin", "http://evil.example.com")
			.header("Access-Control-Request-Method", "GET")
			.exchange()
			.expectHeader().doesNotExist("Access-Control-Allow-Credentials");
	}
}
