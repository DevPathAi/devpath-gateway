# devpath-gateway

**DevPath AI** API Gateway — 모든 클라이언트 트래픽의 단일 진입점입니다.

## 역할

- Spring Cloud Gateway (WebFlux) 기반 엣지 라우팅
- OAuth2 (GitHub) 로그인 + JWT 발급/검증
- 도메인 서비스(`devpath-*-svc`) 라우팅
- OTel 샘플링 (10~30%)

아키텍처 배경: [documents/03_프로젝트_아키텍처_정의서](https://github.com/DevPathAi/documents/blob/main/03_프로젝트_아키텍처_정의서.md)

## 구성

- Spring Boot 4.0.x · Spring Cloud 2025.1.x · Java 21 · Gradle (Kotlin DSL)
- 스타터: Gateway Server (WebFlux), OAuth2 Client, Actuator

## 빌드 / 실행

```bash
./gradlew build
./gradlew bootRun    # 기본 포트 8080
```

라우팅 규칙은 `src/main/resources/application.yml`의 `spring.cloud.gateway.server.webflux.routes`에 추가합니다.

## 개발 규칙

- Git 규칙: [documents/09_Git_규칙_정의서](https://github.com/DevPathAi/documents/blob/main/09_Git_규칙_정의서.md)
- 워크플로우 현황: `docs/project-management/` → [workflow-dashboard](https://devpathai.github.io/workflow-dashboard/)
