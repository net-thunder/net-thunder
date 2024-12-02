package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestIpRequest {

    @NotNull
    private Long nodeId;
    @Pattern(regexp = ValidGroup.IPV4_ADDRESS, message = "IP地址格式错误")
    private String ip;
}
