package com.github.charlemaznable.logback.dendrobe.apollo;

import com.github.charlemaznable.apollo.MockApolloServer;
import com.github.charlemaznable.logback.dendrobe.kafka.KafkaClientManager;
import com.github.charlemaznable.logback.dendrobe.kafka.KafkaClientManagerListener;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static com.github.charlemaznable.core.codec.Json.unJson;
import static com.github.charlemaznable.core.kafka.KafkaClientElf.buildConsumer;
import static com.github.charlemaznable.core.kafka.KafkaClientElf.buildProducer;
import static com.github.charlemaznable.core.kafka.KafkaConfigElf.KAFKA_CONFIG_APOLLO_NAMESPACE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class KafkaAppenderTest implements ApolloUpdaterListener, KafkaClientManagerListener {

    private static final String CLASS_NAME = KafkaAppenderTest.class.getName();

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

    static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    private static KafkaProducer<String, String> kafkaProducer;
    private static KafkaConsumer<String, String> kafkaConsumer;

    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @BeforeAll
    public static void beforeAll() {
        kafka.start();
        val bootstrapServers = kafka.getBootstrapServers();

        val producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = buildProducer(producerConfig);

        val consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumer = buildConsumer(consumerConfig);
        kafkaConsumer.assign(Collections.singleton(new TopicPartition("logback.apollo", 0)));

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(KafkaAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        kafkaProducer.close();
        kafkaConsumer.close();
        kafka.stop();
    }

    @Test
    public void testKafkaAppender() {
        MockApolloServer.setUpMockServer();
        ApolloUpdater.addListener(this);
        KafkaClientManager.addListener(this);

        updated = false;
        configured = false;
        MockApolloServer.addOrModifyProperty(KAFKA_CONFIG_APOLLO_NAMESPACE, "DEFAULT",
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG + "=" + kafka.getBootstrapServers() + "\n");
        MockApolloServer.addOrModifyProperty("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[kafka][console]\n" +
                CLASS_NAME + "[kafka.level]=info\n" +
                CLASS_NAME + "[kafka.name]=DEFAULT\n" +
                CLASS_NAME + "[kafka.topic]=logback.apollo\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root kafka log {}", "1");
        self.info("self kafka log {}", "1");
        await().forever().untilAsserted(() ->
                assertConsumedContent("self kafka log 1"));

        KafkaClientManager.removeListener(this);
        ApolloUpdater.removeListener(this);
        MockApolloServer.tearDownMockServer();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private void assertConsumedContent(String content) {
        while (true) {
            val records = kafkaConsumer.poll(Duration.ofSeconds(1));
            for (val record : records) {
                val valueMap = unJson(record.value());
                assertEquals(content, ((Map<String, String>) valueMap.get("event")).get("message"));
                return;
            }
        }
    }

    @Override
    public void acceptApolloPropertyProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredKafkaClient(String kafkaName) {
        configured = true;
    }
}
