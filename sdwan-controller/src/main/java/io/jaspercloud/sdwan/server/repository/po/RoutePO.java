package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_route")
public class RoutePO extends BaseTenantPO<RoutePO> {

    private String name;
    private String description;
    private String destination;
    private Boolean enable;
}
