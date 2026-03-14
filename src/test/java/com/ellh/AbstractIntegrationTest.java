package com.ellh;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
            .withDatabaseName("ellh_test")
            .withUsername("ellh_user")
            .withPassword("test_password")
            .withReuse(true);

    static final MongoDBContainer MONGODB =
        new MongoDBContainer(DockerImageName.parse("mongo:7"))
            .withReuse(true);

    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        POSTGRES.start();
        MONGODB.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",        POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",   POSTGRES::getUsername);
        registry.add("spring.datasource.password",   POSTGRES::getPassword);
        registry.add("spring.data.mongodb.uri",      MONGODB::getReplicaSetUrl);
        registry.add("spring.data.redis.host",       REDIS::getHost);
        registry.add("spring.data.redis.port",
            () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password",   () -> "");
    }
}
