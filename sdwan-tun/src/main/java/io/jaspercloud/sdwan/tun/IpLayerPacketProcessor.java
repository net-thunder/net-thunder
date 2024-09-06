package io.jaspercloud.sdwan.tun;

public interface IpLayerPacketProcessor {

    void input(IpLayerPacket packet);

    void output(IpLayerPacket packet);
}
