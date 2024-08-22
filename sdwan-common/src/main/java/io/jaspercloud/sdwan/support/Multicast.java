package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.util.IPUtil;

public final class Multicast {

    private Multicast() {

    }

    public static boolean isMulticastIp(String ip) {
        int addr = IPUtil.ip2int(ip);
        return (addr & 0xf0000000) == 0xe0000000;
    }
}
