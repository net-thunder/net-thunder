package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jasper
 * @create 2024/7/12
 */
@EnableConfigurationProperties(SdWanNodeConfig.class)
@Configuration
@SpringBootApplication
public class SdWanApplication {

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SdWanApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public TunSdWanNode sdWanNode(SdWanNodeConfig properties) {
        TunSdWanNode sdWanServer = new TunSdWanNode(properties);
        return sdWanServer;
    }
}
