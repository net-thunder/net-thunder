package io.jaspercloud.sdwan.platform.rpc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TunnelInfo implements Serializable {

    private String controllerServer;
    private String stunServer;
    private String relayServer;
    private String vipCidr;
    private String localVip;
    private int sdwanLocalPort;
    private int p2pLocalPort;
    private int relayLocalPort;
}
