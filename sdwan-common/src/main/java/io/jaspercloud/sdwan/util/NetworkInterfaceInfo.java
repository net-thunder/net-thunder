package io.jaspercloud.sdwan.util;

import lombok.Data;

import java.net.InterfaceAddress;

@Data
public class NetworkInterfaceInfo {

    private String ethName;
    private Integer index;
    private InterfaceAddress interfaceAddress;
    private String hardwareAddress;
    private String ip;
}
