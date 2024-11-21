package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppCheckResponse {

    private String path;
    private String md5;
}
