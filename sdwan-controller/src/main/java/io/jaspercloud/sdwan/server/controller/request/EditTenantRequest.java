package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.common.ValidCheck;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.regex.Matcher;

@Getter
@Setter
public class EditTenantRequest implements ValidCheck {

    @NotNull(groups = {ValidGroup.Update.class, ValidGroup.Delete.class})
    private Long id;
    @Pattern(regexp = ValidGroup.NAME, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String name;
    private String description;
    @Pattern(regexp = ValidGroup.CODE, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String code;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String cidr;
    @Pattern(regexp = ValidGroup.UNAME_PWD, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String username;
    private String password;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<String> stunServerList;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<String> relayServerList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean enable;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean nodeGrant;

    @Override
    public void check() {
        try {
            Cidr.parseCidr(cidr);
        } catch (Exception e) {
            throw new ProcessException("地址池格式错误: " + cidr);
        }
        if (null != password) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(ValidGroup.UNAME_PWD);
            Matcher matcher = pattern.matcher(password);
            if (!matcher.find()) {
                throw new ProcessException("密码格式错误: " + ValidGroup.UNAME_PWD);
            }
        }
        stunServerList.forEach(socket -> {
            try {
                SocketAddressUtil.parse(socket);
            } catch (Exception e) {
                throw new ProcessException("stunServer格式错误: " + socket);
            }
        });
        relayServerList.forEach(socket -> {
            try {
                SocketAddressUtil.parse(socket);
            } catch (Exception e) {
                throw new ProcessException("relayServer格式错误: " + socket);
            }
        });
    }
}
