package io.jaspercloud.sdwan.server.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantEntity<T extends Model<?>> extends Model<T> {

    private Long tenantId;
}
