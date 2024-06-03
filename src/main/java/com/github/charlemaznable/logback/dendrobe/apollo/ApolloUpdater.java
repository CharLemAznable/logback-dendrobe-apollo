package com.github.charlemaznable.logback.dendrobe.apollo;

import com.ctrip.framework.apollo.ConfigService;
import com.github.charlemaznable.logback.dendrobe.HotUpdater;
import com.github.charlemaznable.logback.dendrobe.LogbackDendrobeListener;
import com.google.auto.service.AutoService;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.slf4j.helpers.Reporter;

import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.charlemaznable.core.lang.Propertiess.parseStringToProperties;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.Executors.newFixedThreadPool;

@AutoService(HotUpdater.class)
public final class ApolloUpdater implements HotUpdater {

    private static final CopyOnWriteArrayList<ApolloUpdaterListener> listeners = new CopyOnWriteArrayList<>();
    private static final AsyncEventBus notifyBus;

    private static final String APOLLO_NAMESPACE_KEY = "logback.apollo.namespace";
    private static final String APOLLO_PROPERTY_NAME_KEY = "logback.apollo.propertyName";

    private static final String DEFAULT_APOLLO_NAMESPACE = "Logback";
    private static final String DEFAULT_APOLLO_PROPERTY_NAME = "default";

    private LogbackDendrobeListener dendrobeListener;

    static {
        notifyBus = new AsyncEventBus(ApolloUpdaterListener.class.getName(), newFixedThreadPool(1));
        notifyBus.register(new Object() {
            @Subscribe
            public void notifyListeners(Properties properties) {
                for (val listener : listeners) {
                    try {
                        listener.acceptApolloPropertyProperties(properties);
                    } catch (Exception t) {
                        Reporter.error("listener error:", t);
                    }
                }
            }
        });
    }

    public static void addListener(ApolloUpdaterListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ApolloUpdaterListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void initialize(LogbackDendrobeListener listener, Properties config) {
        this.dendrobeListener = listener;

        // 本地配置apollo配置坐标
        val namespace = config.getProperty(APOLLO_NAMESPACE_KEY, DEFAULT_APOLLO_NAMESPACE);
        val propertyName = config.getProperty(APOLLO_PROPERTY_NAME_KEY, DEFAULT_APOLLO_PROPERTY_NAME);

        new Thread(() -> {
            // apollo配置覆盖默认配置
            val apolloConfig = ConfigService.getConfig(namespace);
            accept(apolloConfig.getProperty(propertyName, ""));

            apolloConfig.addChangeListener(e ->
                    accept(e.getChange(propertyName).getNewValue()), newHashSet(propertyName));
        }).start();
    }

    public void accept(String propertyValue) {
        val properties = parseStringToProperties(propertyValue);
        this.dendrobeListener.reset(properties);
        notifyBus.post(properties);
    }
}
