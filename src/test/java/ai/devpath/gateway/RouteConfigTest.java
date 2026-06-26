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
}
