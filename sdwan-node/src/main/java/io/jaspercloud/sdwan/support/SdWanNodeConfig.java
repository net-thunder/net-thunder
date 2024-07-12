package io.jaspercloud.sdwan.support;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "node")
public class SdWanNodeConfig {

    private String controllerServer;
    private int connectTimeout;
    private long heartTime;

    private String stunServer;
    private String relayServer;

    private String localAddress;
    private int p2pPort;
    private long p2pHeartTime;

    private String tunName;
    private int mtu;
}