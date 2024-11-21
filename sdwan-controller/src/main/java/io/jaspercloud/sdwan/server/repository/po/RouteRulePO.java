package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_route_rule")
public class RouteRulePO extends BaseTenantPO<RouteRulePO> {

    private String name;
    private String description;
    private String strategy;
    private String direction;
    @TableField("rule_list")
    private String ruleList;
    private Integer level;
    private Boolean enable;
}
