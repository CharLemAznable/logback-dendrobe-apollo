package com.github.charlemaznable.logback.dendrobe.apollo;

import com.github.charlemaznable.apollo.MockApolloServer;
import com.github.charlemaznable.core.vertx.VertxElf;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxManager;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxManagerListener;
import com.hazelcast.config.Config;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

import static com.github.charlemaznable.core.vertx.VertxClusterConfigElf.VERTX_CLUSTER_CONFIG_APOLLO_NAMESPACE;
import static com.github.charlemaznable.core.vertx.VertxOptionsConfigElf.VERTX_OPTIONS_APOLLO_NAMESPACE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class VertxAppenderTest implements ApolloUpdaterListener, VertxManagerListener {

    private static final String CLASS_NAME = VertxAppenderTest.class.getName();

    private static Vertx vertx;
    private static String lastEventMessage;
    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @BeforeAll
    public static void beforeAll() {
        val vertxOptions = new VertxOptions();
        vertxOptions.setWorkerPoolSize(10);
        val hazelcastConfig = new Config();
        hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        vertxOptions.setClusterManager(new HazelcastClusterManager(hazelcastConfig));
        vertx = VertxElf.buildVertx(vertxOptions);
        vertx.eventBus().consumer("logback.apollo",
                (Handler<Message<JsonObject>>) event -> {
                    try {
                        lastEventMessage = event.body().getJsonObject("event").getString("message");
                    } catch (Exception e) {
                        lastEventMessage = null;
                    }
                });

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(VertxAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        VertxElf.closeVertx(vertx);
    }

    @Test
    public void testVertxAppender() {
        MockApolloServer.setUpMockServer();
        ApolloUpdater.addListener(this);
        VertxManager.addListener(this);

        updated = false;
        configured = false;
        MockApolloServer.addOrModifyProperty(VERTX_CLUSTER_CONFIG_APOLLO_NAMESPACE, "DEFAULT", "" +
                "hazelcast:\n" +
                "  network:\n" +
                "    join:\n" +
                "      multicast:\n" +
                "        enabled: true\n");
        MockApolloServer.addOrModifyProperty(VERTX_OPTIONS_APOLLO_NAMESPACE, "DEFAULT", "" +
                "workerPoolSize=42\n" +
                "clusterManager=@com.github.charlemaznable.vertx.config.ApolloHazelcastClusterManager(DEFAULT)\n");
        MockApolloServer.addOrModifyProperty("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[vertx]\n" +
                CLASS_NAME + "[vertx.level]=info\n" +
                CLASS_NAME + "[vertx.name]=DEFAULT\n" +
                CLASS_NAME + "[vertx.address]=logback.apollo\n" +
                CLASS_NAME + "[console.level]=off\n" +
                CLASS_NAME + "[eql.level]=off\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root vertx log {}", "old");
        self.info("self vertx log {}", "old");
        await().timeout(Duration.ofSeconds(20)).untilAsserted(() ->
            assertEquals("self vertx log old", lastEventMessage));

        VertxManager.removeListener(this);
        ApolloUpdater.removeListener(this);
        MockApolloServer.tearDownMockServer();
    }

    @Override
    public void acceptApolloPropertyProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredVertx(String vertxName) {
        configured = true;
    }
}
