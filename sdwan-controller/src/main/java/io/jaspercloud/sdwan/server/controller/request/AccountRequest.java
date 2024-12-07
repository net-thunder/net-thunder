package io.jaspercloud.sdwan.server.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {

    @NotEmpty(groups = {LoginGroup.class})
    private String username;
    @NotEmpty(groups = {LoginGroup.class, UpdatePasswordGroup.class})
    private String password;
    @NotEmpty(groups = {UpdatePasswordGroup.class})
    private String newPassword;

    public interface LoginGroup {

    }

    public interface UpdatePasswordGroup {

    }
}
