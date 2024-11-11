package io.jaspercloud.sdwan.tranport.config;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NodeConfig {

    private String mac;
    private String vip;
    private List<RouteConfig> routeConfigList;
    private List<VNATConfig> vnatConfigList;
}
