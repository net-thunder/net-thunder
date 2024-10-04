package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.support.AddressUri;

import javax.crypto.SecretKey;

public interface DataTransport {

    long order();

    AddressUri addressUri();

    void sendPingOneWay(String tranId);

    void setSecretKey(SecretKey secretKey);

    void transfer(String vip, byte[] bytes);

    byte[] decode(byte[] bytes);
}
