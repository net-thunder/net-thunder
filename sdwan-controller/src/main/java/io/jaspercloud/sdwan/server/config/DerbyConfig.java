package io.jaspercloud.sdwan.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.DerbyISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.LimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.datasource.dbType", havingValue = "derby")
@Configuration
public class DerbyConfig {

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.path}") String path) {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", path);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        return dataSource;
    }

    @Bean
    public ISqlInjector sqlInjector() {
        return new DerbyISqlInjector();
    }

    @Bean
    public LimitInterceptor limitInterceptor() {
        return new LimitInterceptor();
    }
}
