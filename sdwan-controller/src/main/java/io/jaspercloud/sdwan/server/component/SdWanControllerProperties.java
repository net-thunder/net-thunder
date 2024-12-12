package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.RelayServerConfig;
import io.jaspercloud.sdwan.tranport.ControllerServerConfig;
import io.jaspercloud.sdwan.tranport.StunServerConfig;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "sdwan")
public class SdWanControllerProperties {

    private SdwanHttpServerConfig httpServer;
    private ControllerServerConfig controllerServer;
    private RelayServerConfig relayServer;
    private StunServerConfig stunServer;

    @Getter
    @Setter
    public static class SdwanHttpServerConfig {

        private String controllerAddress;
        private String storage;
    }
}
