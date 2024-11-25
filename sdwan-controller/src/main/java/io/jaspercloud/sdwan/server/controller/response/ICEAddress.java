package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ICEAddress {

    private String type;
    private String address;
    private String server;
}
