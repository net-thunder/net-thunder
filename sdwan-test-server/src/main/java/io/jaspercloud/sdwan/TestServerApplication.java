package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.config.TestSdWanControllerProperties;
import io.jaspercloud.sdwan.tranport.RelayServer;
import io.jaspercloud.sdwan.tranport.ControllerServer;
import io.jaspercloud.sdwan.tranport.StunServer;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jasper
 * @create 2024/7/2
 */
@EnableConfigurationProperties(TestSdWanControllerProperties.class)
@Configuration
@SpringBootApplication
public class TestServerApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(TestServerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public ControllerServer sdWanServer(TestSdWanControllerProperties properties) {
        SdWanDataService dataService = new LocalConfigSdWanDataService(properties.getSdwan());
        ControllerServer controllerServer = new ControllerServer(properties.getSdwan(), dataService, () -> new ChannelInboundHandlerAdapter());
        return controllerServer;
    }

    @Bean
    public RelayServer relayServer(TestSdWanControllerProperties properties) {
        return new RelayServer(properties.getRelay(), () -> new ChannelInboundHandlerAdapter());
    }

    @Bean
    public StunServer stunServer(TestSdWanControllerProperties properties) {
        return new StunServer(properties.getStun(), () -> new ChannelInboundHandlerAdapter());
    }
}
