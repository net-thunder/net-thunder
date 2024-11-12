package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_node")
public class NodePO extends BaseTenantPO<NodePO> {

    private String name;
    private String description;
    private String mac;
    private String vip;
    private Boolean enable;
}
