package io.jaspercloud.sdwan.server.controller.request;

import cn.hutool.core.lang.PatternPool;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.common.ValidCheck;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleStrategyEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditRouteRuleRequest implements ValidCheck {

    @NotNull(groups = {ValidGroup.Update.class, ValidGroup.Delete.class})
    private Long id;
    @Pattern(regexp = ValidGroup.NAME, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String name;
    private String description;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private RouteRuleStrategyEnum strategy;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private RouteRuleDirectionEnum direction;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<String> ruleList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Integer level;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<Long> groupIdList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean enable;

    @Override
    public void check() {
        ruleList.forEach(rule -> {
            if (PatternPool.IPV4.matcher(rule).find()) {
                //ipv4
            } else if (rule.contains("-")) {
                checkIpRange(rule);
            } else {
                try {
                    Cidr.parseCidr(rule);
                } catch (Exception e) {
                    throw new ProcessException("规则格式错误: " + rule);
                }
            }
        });
    }

    private void checkIpRange(String range) {
        String[] split = range.split("-");
        if (split.length != 2) {
            throw new ProcessException("规则格式错误: " + range);
        }
        if (!PatternPool.IPV4.matcher(split[0]).find()) {
            throw new ProcessException("规则格式错误: " + range);
        }
        if (!PatternPool.IPV4.matcher(split[1]).find()) {
            throw new ProcessException("规则格式错误: " + range);
        }
    }
}
