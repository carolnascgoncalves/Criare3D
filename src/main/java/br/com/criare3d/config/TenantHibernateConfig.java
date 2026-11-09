package br.com.criare3d.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantHibernateConfig {

    @Bean
    HibernatePropertiesCustomizer tenantResolverProperty(TenantIdentifierResolver resolver) {
        return properties -> properties.put("hibernate.tenant_identifier_resolver", resolver);
    }
}
