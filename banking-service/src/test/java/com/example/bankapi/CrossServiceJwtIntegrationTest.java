package com.example.bankapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the cross-service contract is real, not mocked:
 * spins up a real auth-service container + MySQL, fetches a real RS256 JWT,
 * points this service's JWKS decoder at that container, then exercises a banking endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CrossServiceJwtIntegrationTest {

    static final Network NETWORK = Network.newNetwork();

    @Container
    static MySQLContainer<?> bankingDb = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("bankdb").withUsername("bankuser").withPassword("bankpass")
            .withNetwork(NETWORK).withNetworkAliases("banking-db");

    @Container
    static MySQLContainer<?> authDb = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("authdb").withUsername("authuser").withPassword("authpass")
            .withNetwork(NETWORK).withNetworkAliases("auth-db");

    @Container
    static GenericContainer<?> authService = new GenericContainer<>(
            buildAuthServiceImage())
            .withNetwork(NETWORK)
            .withNetworkAliases("auth-service")
            .withExposedPorts(8081)
            .withEnv(Map.of(
                    "SPRING_DATASOURCE_URL", "jdbc:mysql://auth-db:3306/authdb?useSSL=false&serverTimezone=UTC",
                    "SPRING_DATASOURCE_USERNAME", "authuser",
                    "SPRING_DATASOURCE_PASSWORD", "authpass"))
            .dependsOn(authDb)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", bankingDb::getJdbcUrl);
        registry.add("spring.datasource.username", bankingDb::getUsername);
        registry.add("spring.datasource.password", bankingDb::getPassword);
        registry.add("app.auth.jwks-uri", () ->
                "http://" + authService.getHost() + ":" + authService.getMappedPort(8081) + "/.well-known/jwks.json");
    }

    @LocalServerPort
    int port;

    final TestRestTemplate rest = new TestRestTemplate();
    final ObjectMapper mapper = new ObjectMapper();

    private static org.testcontainers.images.builder.ImageFromDockerfile buildAuthServiceImage() {
        // Builds auth-service's own Dockerfile from the sibling module so the test
        // exercises the same image that ships in docker-compose, not a stub.
        return new org.testcontainers.images.builder.ImageFromDockerfile()
                .withFileFromPath(".", Path.of("..", "auth-service"));
    }

    private String authBaseUrl() {
        return "http://" + authService.getHost() + ":" + authService.getMappedPort(8081);
    }

    @Test
    void validTokenFromRealAuthServiceIsAcceptedByBankingService() throws IOException {
        String username = "matthew" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Register against the real auth-service container
        Map<String, String> registerBody = Map.of(
                "username", username, "email", username + "@x.com", "password", "supersecret123");
        ResponseEntity<String> registerResponse = rest.postForEntity(
                authBaseUrl() + "/api/auth/register", registerBody, String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String token = mapper.readTree(registerResponse.getBody()).get("token").asText();

        // 2. Call banking-service with that token - it has never seen auth-service's private key
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> accountsResponse = rest.exchange(
                "http://localhost:" + port + "/api/v1/accounts",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(accountsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void tamperedTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer not.a.validtoken");
        ResponseEntity<String> response = rest.exchange(
                "http://localhost:" + port + "/api/v1/accounts",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
