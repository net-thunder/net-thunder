package io.jaspercloud.sdwan.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.support.derby.DerbyISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.LimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.datasource.dbType", havingValue = "derby")
@Configuration
public class DerbyConfig {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> environmentPreparedEvent() {
        return event -> {
            System.out.println();
        };
    }

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.path}") String path,
                                 Environment env) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", path);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        String initSql = env.getProperty("spring.datasource.init-sql");
        if (null != initSql) {
            executeSql(dataSource, initSql);
        }
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

    private void executeSql(DataSource dataSource, String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            throw new ProcessException("sqlFile not found");
        }
        List<String> sqlList = new ArrayList<>();
        try (InputStream stream = new FileInputStream(file)) {
            String text = StreamUtils.copyToString(stream, Charset.forName("utf-8"));
            String[] split = text.split(";");
            for (String sp : split) {
                String sql = sp.replaceAll("--.*", "").trim();
                if (StringUtils.isNotEmpty(sql)) {
                    sqlList.add(sql);
                }
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlList) {
                    try {
                        statement.execute(sql);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }
}
