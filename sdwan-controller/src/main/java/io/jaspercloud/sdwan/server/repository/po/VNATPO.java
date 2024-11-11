package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BaseTenantPO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_vnat")
public class VNATPO extends BaseTenantPO<VNATPO> {

    private String name;
    private String description;
    @TableField("src_cidr")
    private String srcCidr;
    @TableField("dst_cidr")
    private String dstCidr;
    private Boolean enable;
}
