package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.tranport.RelayServerConfig;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import io.jaspercloud.sdwan.tranport.StunServerConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "controller")
public class SdWanControllerProperties {

    private SdWanServerConfig sdwan;
    private RelayServerConfig relay;
    private StunServerConfig stun;
}
