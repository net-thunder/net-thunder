package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.common.ValidCheck;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.support.Cidr;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditRouteRequest implements ValidCheck {

    @NotNull(groups = {ValidGroup.Update.class, ValidGroup.Delete.class})
    private Long id;
    @Pattern(regexp = ValidGroup.NAME, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String name;
    private String description;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String destination;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<Long> nodeIdList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<Long> groupIdList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean enable;

    @Override
    public void check() {
        try {
            Cidr.parseCidr(destination);
        } catch (Exception e) {
            throw new ProcessException("destination格式错误: " + destination);
        }
    }
}
