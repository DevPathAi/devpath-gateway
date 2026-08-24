package ai.devpath.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
class RouteConfigTest {
	@Autowired RouteLocator routes;

	@Test
	void releaseOAuthRouteRequiresPinnedRunHeadersAndRewritesGithubRegistration() {
		ServerWebExchange releaseExchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/oauth2/authorization/github")
				.header("X-Candidate-Spec-Sha256", "a".repeat(64))
				.header("X-Release-Run-Key", "A".repeat(22))
				.build());
		Route release = routes.getRoutes()
			.filter(r -> r.getId().equals("platform-release-oauth"))
			.blockFirst();
		assertThat(release).isNotNull();
		StepVerifier.create(release.getPredicate().apply(releaseExchange))
			.expectNext(true).verifyComplete();

		ServerWebExchange ordinaryExchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/oauth2/authorization/github").build());
		StepVerifier.create(release.getPredicate().apply(ordinaryExchange))
			.expectNext(false).verifyComplete();
		assertThat(release.getFilters()).extracting(Object::toString)
			.anyMatch(value -> value.contains("/oauth2/authorization/release"));
	}

	@Test
	void releaseBrowserRouteRequiresPinnedRunHeaders() {
		ServerWebExchange releaseExchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/v1/release/browser/analytics-events")
				.header("X-Candidate-Spec-Sha256", "b".repeat(64))
				.header("X-Release-Run-Key", "B".repeat(22))
				.build());
		Route release = routes.getRoutes()
			.filter(r -> r.getId().equals("platform-release-browser"))
			.blockFirst();
		assertThat(release).isNotNull();
		StepVerifier.create(release.getPredicate().apply(releaseExchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void platformAuthRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("platform-auth")))
			.expectNext("platform-auth").verifyComplete();
	}

	@Test
	void learningRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("learning")))
			.expectNext("learning").verifyComplete();
	}

	@Test
	void sandboxRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("sandbox")))
			.expectNext("sandbox").verifyComplete();
	}

	@Test
	void aiReviewRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("ai-review")))
			.expectNext("ai-review").verifyComplete();
	}

	@Test
	void communityRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("community")))
			.expectNext("community").verifyComplete();
	}

	@Test
	void lcsRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("lcs")))
			.expectNext("lcs").verifyComplete();
	}

	@Test
	void lcsPathMatchesRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/lcs/snapshots/draft").build());
		Route lcs = routes.getRoutes()
			.filter(r -> r.getId().equals("lcs"))
			.blockFirst();
		assertThat(lcs).isNotNull();
		StepVerifier.create(lcs.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void aiMentorPathMatchesAiReviewRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/ai-mentor/sessions").build());
		Route aiReview = routes.getRoutes()
			.filter(r -> r.getId().equals("ai-review"))
			.blockFirst();
		assertThat(aiReview).isNotNull();
		StepVerifier.create(aiReview.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void notificationRouteIsConfigured() {
		StepVerifier.create(routes.getRoutes().map(r -> r.getId()).filter(id -> id.equals("notification")))
			.expectNext("notification").verifyComplete();
	}

	@Test
	void notificationDevicesPathMatchesRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/notifications/devices").build());
		Route notification = routes.getRoutes()
			.filter(r -> r.getId().equals("notification"))
			.blockFirst();
		assertThat(notification).isNotNull();
		StepVerifier.create(notification.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void adminPathMatchesPlatformAuthRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/admin/users").build());
		Route platform = routes.getRoutes()
			.filter(r -> r.getId().equals("platform-auth"))
			.blockFirst();
		assertThat(platform).isNotNull();
		StepVerifier.create(platform.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void consentsPathMatchesPlatformAuthRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/consents").build());
		Route platform = routes.getRoutes()
			.filter(r -> r.getId().equals("platform-auth"))
			.blockFirst();
		assertThat(platform).isNotNull();
		StepVerifier.create(platform.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}

	@Test
	void betaStatusPathMatchesPlatformAuthRoute() {
		ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/beta/status").build());
		Route platform = routes.getRoutes()
			.filter(r -> r.getId().equals("platform-auth"))
			.blockFirst();
		assertThat(platform).isNotNull();
		StepVerifier.create(platform.getPredicate().apply(exchange))
			.expectNext(true).verifyComplete();
	}
}
