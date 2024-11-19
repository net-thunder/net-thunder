package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_vip_pool")
public class VipPoolPO extends BaseTenantPO<VipPoolPO> {

    private String vip;
    private Boolean used;
}
