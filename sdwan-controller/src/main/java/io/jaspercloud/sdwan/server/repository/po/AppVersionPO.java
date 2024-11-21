package io.jaspercloud.sdwan.server.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.jaspercloud.sdwan.server.repository.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@TableName("biz_app_version")
public class AppVersionPO extends BasePO<AppVersionPO> {

    private String name;
    private String description;
    private String path;
    private String md5;
    private String platform;
    @TableField("create_time")
    private Date createTime;
}
