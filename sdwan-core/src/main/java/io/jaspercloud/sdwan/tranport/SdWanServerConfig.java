package io.jaspercloud.sdwan.tranport;

import lombok.*;

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
    private long heartTimeout = 30 * 1000;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Route {

        private String destination;
        private List<String> nexthop;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class VNAT {

        private String vip;
        private String src;
        private String dst;
    }
}
