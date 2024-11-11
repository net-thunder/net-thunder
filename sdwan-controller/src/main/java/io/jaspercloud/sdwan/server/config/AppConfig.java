package io.jaspercloud.sdwan.server.config;

import io.jaspercloud.sdwan.server.component.*;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SdWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public DatabaseSdWanDataService databaseSdWanDataService() {
        return new DatabaseSdWanDataService();
    }

    @ConditionalOnProperty(value = "sdwan.sdwanServer.enable", havingValue = "true")
    @Bean
    public SdWanServerBean sdwanServer(SdWanControllerProperties properties,
                                       DatabaseSdWanDataService dataService) {
        SdWanServerConfig sdwanServer = properties.getSdwanServer();
        return new SdWanServerBean(sdwanServer, dataService);
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
