package ai.devpath.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 운영 실측(2026-09-17): platform-svc 도 공개 경로에 CORS 를 붙이므로 게이트웨이를 거친 실제
 * GET 응답에 Access-Control-Allow-Origin 이 두 번, Vary 가 두 벌 실려 브라우저가 차단했다
 * (leva.ai.kr/updates 의 초대 회차). 게이트웨이가 엣지에서 중복을 제거해야 한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublicCorsDedupeTest {
  static HttpServer platform;

  @LocalServerPort int port;
  @MockitoBean ReactiveJwtDecoder jwtDecoder;

  @BeforeAll
  static void startPlatformStub() throws IOException {
    platform = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    platform.createContext("/mentor-access/invite-rounds", exchange -> {
      // platform-svc SecurityConfig.publicInviteRounds 가 붙이는 헤더를 그대로 흉내 낸다.
      exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "https://leva.ai.kr");
      exchange.getResponseHeaders().add("Vary", "Origin");
      exchange.getResponseHeaders().add("Vary", "Access-Control-Request-Method");
      exchange.getResponseHeaders().add("Vary", "Access-Control-Request-Headers");
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    platform.start();
  }

  @AfterAll
  static void stopPlatformStub() {
    platform.stop(0);
  }

  @DynamicPropertySource
  static void platformUri(DynamicPropertyRegistry registry) {
    registry.add("PLATFORM_URI", () -> "http://127.0.0.1:" + platform.getAddress().getPort());
  }

  @Test
  void publicInviteRoundsCarryExactlyOneAllowOriginAndUniqueVary() {
    WebTestClient web = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    HttpHeaders headers = web.get().uri("/mentor-access/invite-rounds")
        .header(HttpHeaders.ORIGIN, "https://leva.ai.kr")
        .exchange()
        .expectStatus().isOk()
        .returnResult(String.class)
        .getResponseHeaders();

    assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .containsExactly("https://leva.ai.kr");
    List<String> vary = headers.get(HttpHeaders.VARY);
    assertThat(vary).isNotNull();
    assertThat(vary).doesNotHaveDuplicates();
  }
}
