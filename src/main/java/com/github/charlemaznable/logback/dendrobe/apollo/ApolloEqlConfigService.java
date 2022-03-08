package com.github.charlemaznable.logback.dendrobe.apollo;

import com.ctrip.framework.apollo.ConfigService;
import com.github.charlemaznable.eql.apollo.EqlApolloConfig;
import com.github.charlemaznable.logback.dendrobe.eql.EqlConfigService;
import com.google.auto.service.AutoService;
import lombok.val;
import org.n3r.eql.config.EqlConfig;

import static com.github.charlemaznable.core.lang.Propertiess.parseStringToProperties;
import static com.github.charlemaznable.core.lang.Propertiess.tryDecrypt;
import static com.github.charlemaznable.eql.apollo.EqlApolloConfig.EQL_CONFIG_NAMESPACE;

@AutoService(EqlConfigService.class)
public final class ApolloEqlConfigService implements EqlConfigService {

    @Override
    public String getEqlConfigValue(String configKey) {
        return ConfigService.getConfig(EQL_CONFIG_NAMESPACE).getProperty(configKey, "");
    }

    @Override
    public EqlConfig parseEqlConfig(String configKey, String configValue) {
        val properties = tryDecrypt(parseStringToProperties(configValue), configKey);
        if (properties.isEmpty()) return null;
        return new EqlApolloConfig(properties, configKey);
    }
}
