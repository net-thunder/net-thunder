package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.support.Referenced;

public interface IpPacket extends Referenced {

    short getVersion();

    String getSrcIP();

    String getDstIP();
}
