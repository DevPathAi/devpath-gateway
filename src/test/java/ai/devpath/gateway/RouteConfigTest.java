package ai.devpath.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
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
}
