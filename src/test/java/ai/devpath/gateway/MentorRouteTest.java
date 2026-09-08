package ai.devpath.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MentorRouteTest {

  @LocalServerPort int port;

  @MockitoBean ReactiveJwtDecoder jwtDecoder;

  WebTestClient web;

  @BeforeEach
  void setUp() {
    web = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    when(jwtDecoder.decode("active-token")).thenReturn(Mono.just(jwt("active-token", "ACTIVE")));
    when(jwtDecoder.decode("waitlisted-token"))
        .thenReturn(Mono.just(jwt("waitlisted-token", "WAITLISTED")));
    when(jwtDecoder.decode("missing-token")).thenReturn(Mono.just(jwt("missing-token", null)));
    when(jwtDecoder.decode("unknown-token"))
        .thenReturn(Mono.just(jwt("unknown-token", "PAUSED")));
  }

  @Test
  void mentorSessionsRequireJwt() {
    web.post().uri("/ai-mentor/sessions").exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void activeMentorSessionMatchesRoute() {
    web.post().uri("/ai-mentor/sessions")
        .header(HttpHeaders.AUTHORIZATION, "Bearer active-token")
        .exchange()
        .expectStatus().value(MentorRouteTest::assertGatewayMatchedRoute);
  }

  @Test
  void waitlistedMentorSessionIsForbidden() {
    assertForbidden("waitlisted-token");
  }

  @Test
  void missingMentorAccessClaimIsForbidden() {
    assertForbidden("missing-token");
  }

  @Test
  void unknownMentorAccessClaimIsForbidden() {
    assertForbidden("unknown-token");
  }

  private void assertForbidden(String token) {
    web.post().uri("/ai-mentor/sessions")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isForbidden();
  }

  private static void assertGatewayMatchedRoute(int status) {
    assertThat(status)
        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value())
        .isNotEqualTo(HttpStatus.FORBIDDEN.value())
        .isNotEqualTo(HttpStatus.NOT_FOUND.value());
  }

  private static Jwt jwt(String token, String mentorAccess) {
    Instant now = Instant.now();
    Jwt.Builder jwt = Jwt.withTokenValue(token)
        .header("alg", "HS256")
        .subject("42")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(600))
        .claim("scope", "ROLE_LEARNER");
    if (mentorAccess != null) {
      jwt.claim("mentor_access", mentorAccess);
    }
    return jwt.build();
  }
}
