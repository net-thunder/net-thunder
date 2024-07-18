package io.jaspercloud.sdwan.support;

import lombok.*;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SdWanNodeConfig {

    private String controllerServer = "127.0.0.1:1800";
    private int connectTimeout = 3 * 1000;
    private long heartTime = 15 * 1000;

    private String stunServer = "127.0.0.1:2478";
    private String relayServer = "127.0.0.1:3478";

    private String localAddress;
    private int p2pPort = 0;
    private int relayPort = 0;
    private long p2pHeartTime = 10 * 1000;
    private boolean onlyRelayTransport = false;

    private String tunName = "net-thunder";
    private int mtu = 1440;
}
