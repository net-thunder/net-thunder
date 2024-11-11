package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_node_vnat")
public class NodeVNATPO extends BaseTenantPO<NodeVNATPO> {

    @TableField("node_id")
    private Long nodeId;
    @TableField("vnat_id")
    private Long vnatId;
}
