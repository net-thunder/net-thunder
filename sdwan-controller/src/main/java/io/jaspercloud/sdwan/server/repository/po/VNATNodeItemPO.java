package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_vnat_node_item")
public class VNATNodeItemPO extends BaseTenantPO<VNATNodeItemPO> {

    @TableField("vnat_id")
    private Long vnatId;
    @TableField("node_id")
    private Long nodeId;
}
