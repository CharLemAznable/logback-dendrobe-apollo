package com.github.charlemaznable.logback.dendrobe.apollo;

import com.ctrip.framework.apollo.ConfigService;
import com.github.charlemaznable.apollo.MockApolloServer;
import com.github.charlemaznable.eql.apollo.Aql;
import com.github.charlemaznable.logback.dendrobe.apollo.log.ErrorLog;
import com.github.charlemaznable.logback.dendrobe.apollo.log.NotLog;
import com.github.charlemaznable.logback.dendrobe.apollo.log.SimpleLog;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class EqlAppenderTest implements ApolloUpdaterListener {

    private static final String CLASS_NAME = EqlAppenderTest.class.getName();

    private static final String DB0 = "db0";
    private static final String CREATE_TABLE_SIMPLE_LOG = "" +
            "create table `simple_log` (" +
            "  `log_id` bigint not null," +
            "  `log_content` text," +
            "  `log_date` datetime(3)," +
            "  `log_date_time` datetime(3)," +
            "  primary key (`log_id`)" +
            ");\n";
    private static final String SELECT_SIMPLE_LOGS = "" +
            "select log_id, log_content, log_date, log_date_time from simple_log order by log_id";
    private static final DockerImageName mysqlImageName = DockerImageName.parse("mysql:5.7.34");
    private static final MySQLContainer<?> mysql0 = new MySQLContainer<>(mysqlImageName).withDatabaseName(DB0);
    private static Logger root;
    private static Logger self;
    private boolean updated;

    @BeforeAll
    public static void beforeAll() {
        mysql0.start();

        MockApolloServer.setUpMockServer();
        MockApolloServer.addOrModifyProperty("EqlConfig", DB0, "" +
                "driver=com.mysql.cj.jdbc.Driver\n" +
                "url=" + mysql0.getJdbcUrl() + "\n" +
                "user=" + mysql0.getUsername() + "\n" +
                "password=" + mysql0.getPassword() + "\n");
        await().forever().until(() ->
                isNotBlank(ConfigService.getConfig("EqlConfig").getProperty(DB0, "")));

        new Aql(DB0).execute(CREATE_TABLE_SIMPLE_LOG);

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(EqlAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        MockApolloServer.tearDownMockServer();
        mysql0.stop();
    }

    @Test
    public void testEqlAppender() {
        ApolloUpdater.addListener(this);

        updated = false;
        val sql = "insert into simple_log (log_id,log_content,log_date,log_date_time) values(#event.westId#,concat('(', #property.miner#, '|', ifnull(#mdc.tenantId#, ''), '|', ifnull(#mdc.tenantCode#, ''), ')', #event.message#, #event.exception#),current_timestamp(),current_timestamp())";
        MockApolloServer.addOrModifyProperty("Logback", "test", "" +
                "context.property[miner]=test\n" +
                "root[eql.level]=info\n" +
                "root[eql.connection]=\n" +
                CLASS_NAME + "[appenders]=[eql]\n" +
                CLASS_NAME + "[eql.level]=info\n" +
                CLASS_NAME + "[eql.connection]=" + DB0 + "\n" +
                CLASS_NAME + "[eql.sql]=" + sql + "\n" +
                CLASS_NAME + "[console.level]=off\n" +
                CLASS_NAME + "[vertx.level]=off\n");
        await().forever().until(() -> updated);

        root.info("no db log");
        self.info("no db log");

        root.info("no db log null: {}", (Object) null);
        self.info("no db log null: {}", (Object) null);

        val notLog = new NotLog();
        notLog.setLogId("1000");
        notLog.setLogContent("no db log not log");
        root.info("no db log not log: {}", notLog);
        self.info("no db log not log: {}", notLog);

        val errorLog = new ErrorLog();
        errorLog.setLogId("1000");
        errorLog.setLogContent("no db log error log");
        root.info("no db log error log: {}", errorLog);
        self.info("no db log error log: {}", errorLog);

        val simpleLog = new SimpleLog();
        simpleLog.setLogId("1000");
        simpleLog.setLogContent("simple log");
        simpleLog.setLogDate(new Date());
        simpleLog.setLogDateTime(DateTime.now());
        root.info("simple log: {} >> actual ignored", simpleLog);
        self.info("simple log: {}", simpleLog);

        await().timeout(Duration.ofSeconds(30)).pollDelay(Duration.ofSeconds(3)).until(() -> {
            List<Object> simpleLogs = new Aql(DB0).execute(SELECT_SIMPLE_LOGS);
            return 4 == simpleLogs.size();
        });

        List<SimpleLog> simpleLogs = new Aql(DB0).returnType(SimpleLog.class)
                .execute(SELECT_SIMPLE_LOGS);

        val querySimpleLog = simpleLogs.get(0);
        assertEquals(simpleLog.getLogId(), querySimpleLog.getLogId());
        assertEquals(simpleLog.getLogContent(), querySimpleLog.getLogContent());
        assertEquals(simpleLog.getLogDate(), querySimpleLog.getLogDate());
        assertEquals(simpleLog.getLogDateTime(), querySimpleLog.getLogDateTime());

        val queryNoDbLog = simpleLogs.get(1);
        assertEquals("(test||)no db log", queryNoDbLog.getLogContent());

        val queryNoDbLogNull = simpleLogs.get(2);
        assertEquals("(test||)no db log null: null", queryNoDbLogNull.getLogContent());

        val queryNoDbLogNotLog = simpleLogs.get(3);
        assertEquals("(test||)no db log not log: " + notLog, queryNoDbLogNotLog.getLogContent());

        ApolloUpdater.removeListener(this);
    }

    @Override
    public void acceptApolloPropertyProperties(Properties properties) {
        updated = true;
    }
}
