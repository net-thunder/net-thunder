package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditNodeRequest {

    @NotNull(groups = {ValidGroup.Update.class, ValidGroup.Delete.class})
    private Long id;
    @Pattern(regexp = ValidGroup.NAME, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String name;
    private String description;
    @Pattern(regexp = ValidGroup.MAC_ADDRESS, groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private String mac;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean mesh;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private List<Long> groupIdList;
    @NotNull(groups = {ValidGroup.Add.class, ValidGroup.Update.class})
    private Boolean enable;
}
