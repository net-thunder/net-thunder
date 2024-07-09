package io.jaspercloud.sdwan.tranport;

import lombok.*;

import java.util.List;
import java.util.Map;

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

    private int port;
    private String vipCidr;
    private Map<String, String> fixedVipMap;
    private long heartTimeout;
    private List<Route> routeList;

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
