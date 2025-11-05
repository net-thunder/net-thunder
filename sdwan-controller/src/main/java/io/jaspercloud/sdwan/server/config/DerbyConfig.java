package io.jaspercloud.sdwan.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.DerbyISqlInjector;
import io.jaspercloud.sdwan.server.support.derby.LimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.SQLSyntaxErrorException;

@Slf4j
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.datasource.dbType", havingValue = "derby")
@Configuration
public class DerbyConfig {

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.path}") String path) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", path);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        initDatasource(dataSource);
        return dataSource;
    }

    private void initDatasource(DruidDataSource dataSource) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.afterPropertiesSet();
        try {
            jdbcTemplate.queryForRowSet("select count(*) from biz_account");
        } catch (BadSqlGrammarException e) {
            SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) e.getCause();
            int errorCode = sqlSyntaxErrorException.getErrorCode();
            if (30000 != errorCode) {
                throw e;
            }
            ClassPathResource resource = new ClassPathResource("sql/init-derby-schema.sql");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (InputStream inputStream = resource.getInputStream()) {
                StreamUtils.copyRange(inputStream, outputStream, 0, resource.contentLength());
            }
            String text = new String(outputStream.toByteArray());
            String[] split = text.split(";");
            for (String sp : split) {
                String sql = sp.replaceAll("^\r\n$", "");
                if (StringUtils.isEmpty(sql)) {
                    continue;
                }
                jdbcTemplate.execute(sql);
            }
        }
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
