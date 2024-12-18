package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditAppVersionRequest {

    @NotNull(groups = {ValidGroup.Update.class, ValidGroup.Delete.class})
    private Long id;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String name;
    private String description;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String path;
    @NotEmpty(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String platform;
}
