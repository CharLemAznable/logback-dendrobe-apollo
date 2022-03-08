package com.github.charlemaznable.logback.dendrobe.apollo;

import com.github.charlemaznable.logback.dendrobe.impl.DefaultVertxOptionsService;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxOptionsService;
import com.github.charlemaznable.vertx.config.VertxOptionsConfigElf;
import com.google.auto.service.AutoService;

@AutoService(VertxOptionsService.class)
public final class ApolloVertxOptionsService extends DefaultVertxOptionsService {

    @Override
    public String getVertxOptionsValue(String configKey) {
        return VertxOptionsConfigElf.getApolloProperty(configKey);
    }
}
