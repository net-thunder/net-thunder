package io.jaspercloud.sdwan;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@Getter
@Setter
public class SdWanNodeConfig {

    private String controllerServer;
    private int connectTimeout;
    private long heartTime;

    private String stunServer;
    private String relayServer;

    private String localAddress;
    private int p2pPort;
    private long p2pHeartTime;
}
