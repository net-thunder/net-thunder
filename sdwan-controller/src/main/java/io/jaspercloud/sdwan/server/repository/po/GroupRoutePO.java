package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_group_route")
public class GroupRoutePO extends BaseTenantPO<GroupRoutePO> {

    @TableField("group_id")
    private Long groupId;
    @TableField("route_id")
    private Long routeId;
}
