package io.jaspercloud.sdwan.server.config;

import io.jaspercloud.sdwan.server.component.*;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SdWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SdWanDataService dataService() {
        return new DatabaseSdWanDataService();
    }

    @ConditionalOnProperty(value = "sdwan.controllerServer.enable", havingValue = "true")
    @Bean
    public ControllerServerBean controllerServer(SdWanControllerProperties properties,
                                                 SdWanDataService dataService) {
        return new ControllerServerBean(properties.getControllerServer(), dataService);
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
