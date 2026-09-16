package com.laaguidi.SignFlow.config;

import com.laaguidi.SignFlow.config.tenant.DynamicSchemaConnectionProvider;
import com.laaguidi.SignFlow.config.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MultiTenantJpaConfig {

    @Bean
    public HibernatePropertiesCustomizer multiTenantHibernatePropertiesCustomizer(
            DynamicSchemaConnectionProvider connectionProvider,
            TenantIdentifierResolver tenantResolver
    ) {
        return (Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}