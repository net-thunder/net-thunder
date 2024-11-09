package io.jaspercloud.sdwan.server.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tenant extends Model<Tenant> {

    private Long id;
    private String name;
    private String code;
    private String cidr;
    private String username;
    private String password;
    private Boolean enable;
}
