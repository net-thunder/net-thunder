package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BasePO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_tenant")
public class TenantPO extends BasePO<TenantPO> {

    private String name;
    private String description;
    private String code;
    private String cidr;
    @TableField("uname")
    private String username;
    @TableField("pwd")
    private String password;
    private String config;
    private Boolean enable;
}
