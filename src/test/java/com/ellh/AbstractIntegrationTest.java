package com.ellh;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests (*IT.java).
 * Starts PostgreSQL, MongoDB, and Redis containers ONCE per test suite JVM
 * using static fields — containers are shared across all subclasses, not
 * restarted between test classes. This keeps integration test runs fast.
 *
 * NOTE: withReuse(true) is intentionally NOT used here.
 * withReuse(true) requires ~/.testcontainers.properties on the runner, which
 * adds a fragile CI dependency. The static field pattern achieves the same
 * container-sharing benefit without it.
 *
 * HOW TO USE:
 *   class AuthControllerIT extends AbstractIntegrationTest {
 *       @Autowired TestRestTemplate restTemplate;
 *       @Test void myTest() { ... }
 *   }
 *
 * Containers are started when the first IT test class loads and stopped
 * automatically by Testcontainers' JVM shutdown hook.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    // static = shared across ALL subclasses in the same test JVM
    // Containers start once, stay alive for all IT tests, stop on JVM exit
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
            .withDatabaseName("ellh_test")
            .withUsername("ellh_user")
            .withPassword("test_password");

    static final MongoDBContainer MONGODB =
        new MongoDBContainer(DockerImageName.parse("mongo:7"));

    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // Start all three containers before any test in the suite
    static {
        POSTGRES.start();
        MONGODB.start();
        REDIS.start();
    }

    /**
     * Injects Testcontainers connection URLs into Spring's property registry
     * at test startup. Overrides whatever is in application-test.properties.
     * This is how Spring Boot discovers the random ports Testcontainers allocates.
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.mongodb.uri",    MONGODB::getReplicaSetUrl);
        registry.add("spring.data.redis.host",     REDIS::getHost);
        registry.add("spring.data.redis.port",
            () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");
        // Disable Flyway in integration tests — Testcontainers gives a fresh
        // empty database. Flyway will run its migrations automatically when
        // Spring Boot starts, which is correct — no extra config needed.
    }
}
