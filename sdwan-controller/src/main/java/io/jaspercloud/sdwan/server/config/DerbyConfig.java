package io.jaspercloud.sdwan.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.DerbyISqlInjector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@Configuration
public class DerbyConfig {

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.path}") String path) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", path);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        initDerby(dataSource, path);
        return dataSource;
    }

    @Bean
    public ISqlInjector sqlInjector() {
        return new DerbyISqlInjector();
    }

    private void initDerby(DataSource dataSource, String path) throws Exception {
        File dbPathFile = new File(path);
        boolean exists = dbPathFile.exists();
        if (exists) {
            return;
        }
        List<String> sqlList = new ArrayList<>();
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("META-INF/init-schema.sql")) {
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
