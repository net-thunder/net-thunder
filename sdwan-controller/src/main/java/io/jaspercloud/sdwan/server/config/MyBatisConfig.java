package io.jaspercloud.sdwan.server.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {

    @Bean
    public TenantContextHandler tenantContextHandler() {
        return new TenantContextHandler();
    }

    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantContextHandler handler) {
        return new TenantLineInnerInterceptor(handler);
    }
}
