package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_node_rule")
public class NodeRouteRulePO extends BaseTenantPO<NodeRouteRulePO> {

    @TableField("node_id")
    private Long nodeId;
    @TableField("rule_id")
    private Long ruleId;
}
