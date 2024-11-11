package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BasePO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_account")
public class AccountPO extends BasePO<TenantPO> {

    @TableField("uname")
    private String username;
    @TableField("pwd")
    private String password;
    private String role;
}
