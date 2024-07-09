package io.jaspercloud.sdwan.util;

public interface AddressType {

    //本地网卡地址
    String HOST = "host";
    //STUN服务器获得的ip和端口生成
    String SRFLX = "srflx";
    //RELAY服务器获得
    String RELAY = "relay";
}
