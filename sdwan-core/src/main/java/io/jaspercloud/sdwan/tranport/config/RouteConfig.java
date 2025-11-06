package io.jaspercloud.sdwan.tranport.config;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RouteConfig {

    private String name;
    private String destination;
    private List<String> nexthop;
}
