package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.support.Referenced;

public interface IpPacket extends Referenced {

    int Icmp = 1;
    int Tcp = 6;
    int Udp = 17;

    short getVersion();

    int getProtocol();

    String getSrcIP();

    String getDstIP();
}
