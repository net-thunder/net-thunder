package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_route_node_item")
public class RouteNodeItemPO extends BaseTenantPO<RouteNodeItemPO> {

    @TableField("route_id")
    private Long routeId;
    @TableField("node_id")
    private Long nodeId;
}
