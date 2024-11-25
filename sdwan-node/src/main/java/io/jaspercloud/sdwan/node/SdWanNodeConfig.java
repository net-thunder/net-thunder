package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.*;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

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

    private String tenantId = "default";
    private String httpServer = "127.0.0.1:1805";
    private String controllerServer = "127.0.0.1:1800";
    private int connectTimeout = 30 * 1000;
    private long heartTime = 15 * 1000;

    private List<String> stunServerList = Arrays.asList("127.0.0.1:2478");
    private List<String> relayServerList = Arrays.asList("127.0.0.1:3478");

    private String localAddress;
    private int p2pPort = 0;
    private int relayPort = 0;

    private long electionTimeout = 15 * 1000;
    private long iceCheckTime = 15 * 1000;
    private long iceCheckTimeout = 1500;
    private long p2pCheckTime = 3000;
    private long p2pCheckTimeout = 1000;

    private boolean onlyRelayTransport = false;

    private String tunName = "net-thunder";
    private int mtu = 1280;

    private Boolean netMesh = false;
    private Boolean autoReconnect = true;
    private Boolean autoUpdateVersion = true;

    private List<String> ifaceBlackList = Arrays.asList(
            "wt0",
            "wt",
            "utun",
            "tun0",
            "zt",
            "ZeroTier",
            "wg",
            "ts",
            "Tailscale",
            "tailscale",
            "docker",
            "veth",
            "br-",
            "lo"
    );

    public String getHostAddress() {
        try {
            String hostAddress = Arrays.asList(InetAddress.getAllByName(InetAddress.getLocalHost().getHostName()))
                    .stream().filter(e -> !"127.0.0.1".equals(e.getHostAddress()))
                    .findAny()
                    .get().getHostAddress();
            return hostAddress;
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }
}
