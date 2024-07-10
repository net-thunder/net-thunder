package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.adapter.server.SdWanControllerProperties;
import io.jaspercloud.sdwan.tranport.RelayServer;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.jaspercloud.sdwan.tranport.StunServer;
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
@EnableConfigurationProperties(SdWanControllerProperties.class)
@Configuration
@SpringBootApplication
public class TestServerApplication {

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(TestServerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public SdWanServer sdWanServer(SdWanControllerProperties properties) {
        SdWanServer sdWanServer = new SdWanServer(properties.getSdwan(), () -> new ChannelInboundHandlerAdapter());
        return sdWanServer;
    }

    @Bean
    public RelayServer relayServer(SdWanControllerProperties properties) {
        return new RelayServer(properties.getRelay(), () -> new ChannelInboundHandlerAdapter());
    }

    @Bean
    public StunServer stunServer(SdWanControllerProperties properties) {
        return new StunServer(properties.getStun(), () -> new ChannelInboundHandlerAdapter());
    }
}
