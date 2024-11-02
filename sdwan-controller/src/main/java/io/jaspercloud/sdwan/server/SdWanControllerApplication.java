package io.jaspercloud.sdwan.server;

import io.jaspercloud.sdwan.server.component.RelayServerBean;
import io.jaspercloud.sdwan.server.component.SdWanControllerProperties;
import io.jaspercloud.sdwan.server.component.SdWanServerBean;
import io.jaspercloud.sdwan.server.component.StunServerBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SdWanControllerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @ConditionalOnProperty(value = "sdwan.sdwanServer.enable", havingValue = "true")
    @Bean
    public SdWanServerBean sdwanServer(SdWanControllerProperties properties) {
        return new SdWanServerBean(properties.getSdwanServer());
    }

    @ConditionalOnProperty(value = "sdwan.relayServer.enable", havingValue = "true")
    @Bean
    public RelayServerBean relayServer(SdWanControllerProperties properties) {
        return new RelayServerBean(properties.getRelayServer());
    }

    @ConditionalOnProperty(value = "sdwan.stunServer.enable", havingValue = "true")
    @Bean
    public StunServerBean stunServer(SdWanControllerProperties properties) {
        return new StunServerBean(properties.getStunServer());
    }
}
