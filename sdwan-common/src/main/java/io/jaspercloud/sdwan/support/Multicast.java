package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.util.IPUtil;

public final class Multicast {

    private Multicast() {

    }

    public static boolean isMulticastIp(String ip) {
        long addr = IPUtil.ip2long(ip);
        if (-1 == addr) {
            // 255.255.255.255
            return true;
        }
        return (addr & 0xf0000000) == 0xe0000000;
    }
}
