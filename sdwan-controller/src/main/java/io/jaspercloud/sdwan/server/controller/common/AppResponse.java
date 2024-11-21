package io.jaspercloud.sdwan.server.controller.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppResponse<T> {

    private Integer code;
    private T data;
}
