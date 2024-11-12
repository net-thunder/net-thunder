package io.jaspercloud.sdwan.tranport.config;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RouteRuleConfig {

    private DirectionEnum direction;
    private List<String> ruleList;
}
