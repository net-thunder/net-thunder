package io.jaspercloud.sdwan.platform.rpc;

public interface WinSvcRpc {

    void createService(String serviceName, String path);

    void startService(String serviceName);

    void stopService(String serviceName);

    void deleteService(String serviceName);

    int queryServiceStatus(String serviceName);
}
