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
}
