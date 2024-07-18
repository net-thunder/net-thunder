package io.jaspercloud.sdwan.adapter;

import io.jaspercloud.sdwan.adapter.server.RelayServerBean;
import io.jaspercloud.sdwan.adapter.server.SdWanControllerProperties;
import io.jaspercloud.sdwan.adapter.server.SdWanServerBean;
import io.jaspercloud.sdwan.adapter.server.StunServerBean;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@EnableConfigurationProperties(SdWanControllerProperties.class)
@Configuration
@SpringBootApplication
public class SdWanControllerApplication {

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SdWanControllerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public SdWanServerBean sdWanServer(SdWanControllerProperties properties) {
        return new SdWanServerBean(properties.getSdwan());
    }

    @Bean
    public RelayServerBean relayServer(SdWanControllerProperties properties) {
        return new RelayServerBean(properties.getRelay());
    }

    @Bean
    public StunServerBean stunServer(SdWanControllerProperties properties) {
        return new StunServerBean(properties.getStun());
    }
}
