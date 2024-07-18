package io.jaspercloud.sdwan.tranport;

import lombok.*;

import java.util.Collections;
import java.util.List;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SdWanServerConfig {

    private int port = 1800;
    private String vipCidr = "10.1.0.0/24";
    private List<FixVip> fixedVipList = Collections.emptyList();
    private long heartTimeout = 30 * 1000;
    private List<Route> routeList = Collections.emptyList();

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class FixVip {

        private String mac;
        private String vip;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Route {

        private String destination;
        private List<String> nexthop;
    }
}
