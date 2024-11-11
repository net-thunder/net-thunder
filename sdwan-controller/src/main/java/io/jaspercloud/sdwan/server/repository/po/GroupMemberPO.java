package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_group_member")
public class GroupMemberPO extends BaseTenantPO<GroupMemberPO> {

    @TableField("group_id")
    private Long groupId;
    @TableField("member_id")
    private Long memberId;
}
