package io.jaspercloud.sdwan.server.entity;

import io.jaspercloud.sdwan.server.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account extends BaseEntity {

    private String username;
    private String password;
    private UserRole role;
}
