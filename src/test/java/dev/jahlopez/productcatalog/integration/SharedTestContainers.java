package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

/**
 * Starts one PostgreSQL and Redis pair for the test JVM so cached Spring
 * contexts can continue using the same infrastructure between test classes.
 */
public final class SharedTestContainers {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    private static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        configureDockerSocket();
        POSTGRES.start();
        REDIS.start();
    }

    private SharedTestContainers() {
    }

    public static PostgreSQLContainer postgres() {
        return POSTGRES;
    }

    public static RedisContainer redis() {
        return REDIS;
    }

    private static void configureDockerSocket() {
        if (System.getenv("DOCKER_HOST") != null) {
            return;
        }

        File standardSocket = new File("/var/run/docker.sock");
        File desktopSocket = new File(System.getProperty("user.home") + "/.docker/run/docker.sock");

        if (desktopSocket.exists()) {
            System.setProperty("docker.host", "unix://" + desktopSocket.getAbsolutePath());
        } else if (standardSocket.exists()) {
            System.setProperty("docker.host", "unix:///var/run/docker.sock");
        }
    }
}
