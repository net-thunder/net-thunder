package io.jaspercloud.sdwan.util;

import lombok.Data;

import java.net.InterfaceAddress;

@Data
public class NetworkInterfaceInfo {

    private String name;
    private int index;
    private InterfaceAddress interfaceAddress;
    private String hardwareAddress;
}
