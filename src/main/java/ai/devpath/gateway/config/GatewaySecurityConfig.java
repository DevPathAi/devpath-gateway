package ai.devpath.gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

	@Bean
	public SecretKey jwtSecretKey(@Value("${JWT_SECRET:test-secret-please-change-min-32-bytes-long-0123456789}") String secret) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) { // P1-3: HS256 최소 256비트. platform과 동일 검증.
			throw new IllegalStateException("JWT_SECRET must be >= 32 bytes (HS256), got " + bytes.length);
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	public ReactiveJwtDecoder jwtDecoder(SecretKey key) {
		return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.cors(Customizer.withDefaults()) // P1-6/R6
			.authorizeExchange(ex -> ex
				.pathMatchers("/oauth2/**", "/login/**", "/auth/refresh", "/auth/logout",
						"/v1/release/browser/**",
						"/onboarding/assessments/guest/**", "/actuator/health", "/actuator/health/**").permitAll()
				.pathMatchers(HttpMethod.POST, "/support/public-requests").permitAll()
				.pathMatchers(HttpMethod.GET, "/mentor-access/invite-rounds").permitAll()
				.pathMatchers("/ai-mentor/**").access((authentication, context) -> authentication
					.map(current -> new AuthorizationDecision(
						current instanceof JwtAuthenticationToken jwt
							&& "ACTIVE".equals(jwt.getToken().getClaimAsString("mentor_access")))))
				.anyExchange().authenticated())
			.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
		return http.build();
	}

	// P1-6/R6: SPA(이종 출처)의 쿠키 동반 요청 허용. allowCredentials=true는 와일드카드 origin과 양립 불가 →
	// 반드시 명시 allowlist(env)로 주입. 브라우저가 Set-Cookie 처리.
	@Bean
	public org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource(
			@Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}") String origins,
			@Value("${PUBLIC_CORS_ALLOWED_ORIGINS:https://leva.ai.kr}") String publicOrigins) {
		var trusted = new org.springframework.web.cors.CorsConfiguration();
		trusted.setAllowCredentials(true);
		trusted.setAllowedOrigins(parseOrigins(origins));
		trusted.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		trusted.setAllowedHeaders(java.util.List.of(
				"Authorization",
				"Content-Type",
				"X-Candidate-Spec-Sha256",
				"X-Release-Run-Key",
				"X-Sandbox-Event-Version"));

		var publicApi = new org.springframework.web.cors.CorsConfiguration();
		publicApi.setAllowCredentials(false);
		publicApi.setAllowedOrigins(parseOrigins(publicOrigins));
		publicApi.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
		publicApi.setAllowedHeaders(java.util.List.of("Content-Type"));

		var source = new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/support/public-requests", publicApi);
		source.registerCorsConfiguration("/mentor-access/invite-rounds", publicApi);
		source.registerCorsConfiguration("/**", trusted);
		return source;
	}

	private static java.util.List<String> parseOrigins(String origins) {
		return java.util.Arrays.stream(origins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
	}
}
