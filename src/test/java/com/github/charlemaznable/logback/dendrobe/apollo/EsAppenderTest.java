package com.github.charlemaznable.logback.dendrobe.apollo;

import com.github.charlemaznable.apollo.MockApolloServer;
import com.github.charlemaznable.core.es.EsConfig;
import com.github.charlemaznable.logback.dendrobe.es.EsClientManager;
import com.github.charlemaznable.logback.dendrobe.es.EsClientManagerListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static com.github.charlemaznable.core.es.EsClientElf.buildEsClient;
import static com.github.charlemaznable.core.es.EsClientElf.closeEsClient;
import static com.github.charlemaznable.es.config.EsConfigElf.ES_CONFIG_APOLLO_NAMESPACE;
import static com.google.common.collect.Lists.newArrayList;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
@Slf4j
public class EsAppenderTest implements ApolloUpdaterListener, EsClientManagerListener {

    private static final String CLASS_NAME = EsAppenderTest.class.getName();

    private static final String ELASTICSEARCH_VERSION = "7.17.4";
    private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag(ELASTICSEARCH_VERSION);

    private static final String ELASTICSEARCH_USERNAME = "elastic";
    private static final String ELASTICSEARCH_PASSWORD = "changeme";

    private static ElasticsearchContainer elasticsearch
            = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withPassword(ELASTICSEARCH_PASSWORD);

    private static RestHighLevelClient esClient;

    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        elasticsearch.start();

        val esConfig = new EsConfig();
        esConfig.setUris(newArrayList(elasticsearch.getHttpHostAddress()));
        esConfig.setUsername(ELASTICSEARCH_USERNAME);
        esConfig.setPassword(ELASTICSEARCH_PASSWORD);
        esClient = buildEsClient(esConfig);

        val createIndexRequest = new CreateIndexRequest("logback.apollo");
        val createIndexResponse = esClient.indices()
                .create(createIndexRequest, DEFAULT);
        val openIndexRequest = new OpenIndexRequest("logback.apollo");
        val openIndexResponse = esClient.indices().open(openIndexRequest, DEFAULT);

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(EsAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        closeEsClient(esClient);
        elasticsearch.stop();
    }

    @Test
    public void testEsAppender() {
        MockApolloServer.setUpMockServer();
        ApolloUpdater.addListener(this);
        EsClientManager.addListener(this);

        updated = false;
        configured = false;
        MockApolloServer.addOrModifyProperty(ES_CONFIG_APOLLO_NAMESPACE, "DEFAULT", "" +
                "uris=" + elasticsearch.getHttpHostAddress() + "\n" +
                "username=" + ELASTICSEARCH_USERNAME + "\n" +
                "password=" + ELASTICSEARCH_PASSWORD + "\n");
        MockApolloServer.addOrModifyProperty("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[es]\n" +
                CLASS_NAME + "[es.level]=info\n" +
                CLASS_NAME + "[es.name]=DEFAULT\n" +
                CLASS_NAME + "[es.index]=logback.apollo\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root es log {}", "1");
        self.info("self es log {}", "1");
        await().timeout(Duration.ofSeconds(20)).untilAsserted(() ->
                assertSearchContent("self es log 1"));

        EsClientManager.removeListener(this);
        ApolloUpdater.removeListener(this);
        MockApolloServer.tearDownMockServer();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    @SneakyThrows
    private void assertSearchContent(String content) {
        val searchRequest = new SearchRequest("logback.apollo");
        searchRequest.source(SearchSourceBuilder.searchSource()
                .query(QueryBuilders.matchPhraseQuery("event.message", content)));
        val searchResponse = esClient.search(searchRequest, DEFAULT);
        val searchResponseHits = searchResponse.getHits();
        assertTrue(searchResponseHits.getHits().length > 0);
        val responseMap = searchResponseHits.getAt(0).getSourceAsMap();
        assertEquals(content, ((Map<String, String>) responseMap.get("event")).get("message"));
    }

    @Override
    public void acceptApolloPropertyProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredEsClient(String esName) {
        configured = true;
    }
}
