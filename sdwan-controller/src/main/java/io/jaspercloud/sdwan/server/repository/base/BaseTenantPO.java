package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseTenantPO<T extends Model<?>> extends BasePO<T> {

    @TableField("tenant_id")
    private Long tenantId;
}
