package io.jaspercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String tunName;
    private String ip;
    private int maskBits;

    public String getTunName() {
        return tunName;
    }

    public String getIp() {
        return ip;
    }

    public int getMaskBits() {
        return maskBits;
    }

    public TunAddress(String tunName, String ip, int maskBits) {
        this.tunName = tunName;
        this.ip = ip;
        this.maskBits = maskBits;
    }
}
