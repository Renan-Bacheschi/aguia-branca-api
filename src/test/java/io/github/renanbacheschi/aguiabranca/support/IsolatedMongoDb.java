package io.github.renanbacheschi.aguiabranca.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** MongoDB descartável e exclusivo da JVM de testes; nunca aponta para o banco local. */
public final class IsolatedMongoDb {
    private static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:8"))
            .withExposedPorts(27017)
            .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1")
            .waitingFor(Wait.forLogMessage("(?s).*Waiting for connections.*\\n", 1));
    private IsolatedMongoDb() { }
    public static synchronized void register(DynamicPropertyRegistry registry) {
        if (!MONGO.isRunning()) MONGO.start();
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/aguia_branca_bdd");
    }
}
