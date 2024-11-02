package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.RelayServerConfig;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
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

    private SdWanServerConfig sdwanServer;
    private RelayServerConfig relayServer;
    private StunServerConfig stunServer;
}
