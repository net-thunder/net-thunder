package io.jaspercloud.sdwan.tranport.config;

import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleStrategyEnum;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RouteRuleConfig {

    private RouteRuleStrategyEnum strategy;
    private RouteRuleDirectionEnum direction;
    private List<String> ruleList;
    private Integer level;
}
