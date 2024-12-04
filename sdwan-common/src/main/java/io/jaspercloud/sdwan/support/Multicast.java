package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.util.IPUtil;

public final class Multicast {

    private static final long LimitedBroadcastAddress = IPUtil.ip2long("255.255.255.255");

    private Multicast() {
    }

    public static boolean isMulticastIp(String ip) {
        long addr = IPUtil.ip2long(ip);
        if (LimitedBroadcastAddress == addr) {
            // 255.255.255.255
            return true;
        }
        //224.0.0.0~239.255.255.255
        return (addr & 0xf0000000L) == 0xe0000000L;
    }
}
