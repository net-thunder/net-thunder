package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
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
    private String os;
    @TableField("os_version")
    private String osVersion;
    @TableField("node_version")
    private String nodeVersion;
    private Boolean mesh;
    private Boolean enable;
}
