package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_group_vnat")
public class GroupVNATPO extends BaseTenantPO<GroupVNATPO> {

    @TableField("group_id")
    private Long groupId;
    @TableField("vnat_id")
    private Long vnatId;
}
