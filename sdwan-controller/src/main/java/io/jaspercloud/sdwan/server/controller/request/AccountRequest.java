package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {

    @NotEmpty(groups = {LoginGroup.class})
    private String username;
    @Pattern(regexp = ValidGroup.UNAME_PWD, groups = {LoginGroup.class, UpdatePasswordGroup.class})
    private String password;
    @Pattern(regexp = ValidGroup.UNAME_PWD, groups = {UpdatePasswordGroup.class})
    private String newPassword;

    public interface LoginGroup {

    }

    public interface UpdatePasswordGroup {

    }
}
