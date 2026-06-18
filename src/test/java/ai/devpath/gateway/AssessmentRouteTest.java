package ai.devpath.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 진단 라우팅·공개경로 검증. guest 경로는 인증 없이 통과(상류 미가동이라 401이 아님),
 * 보호 진단 경로는 JWT 없으면 401(엣지 검증).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssessmentRouteTest {

  @LocalServerPort int port;

  private WebTestClient client() {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void guestPathIsPublic_notUnauthorized() {
    client().post().uri("/onboarding/assessments/guest").exchange()
        .expectStatus().value(s -> org.assertj.core.api.Assertions.assertThat(s)
            .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  @Test
  void protectedAssessmentPathIsUnauthorizedWithoutJwt() {
    client().get().uri("/onboarding/assessments/1/next").exchange()
        .expectStatus().isUnauthorized();
  }
}
