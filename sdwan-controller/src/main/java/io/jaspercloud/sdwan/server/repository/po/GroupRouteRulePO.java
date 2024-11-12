package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_group_rule")
public class GroupRouteRulePO extends BaseTenantPO<GroupRouteRulePO> {

    @TableField("group_id")
    private Long groupId;
    @TableField("rule_id")
    private Long ruleId;
}
