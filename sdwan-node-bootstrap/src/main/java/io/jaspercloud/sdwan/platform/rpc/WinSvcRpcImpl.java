package io.jaspercloud.sdwan.platform.rpc;

import io.jaspercloud.sdwan.support.WinSvcUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinSvcRpcImpl implements WinSvcRpc {

    @Override
    public void createService(String serviceName, String path) {
        log.info("createService: serviceName={}, path={}", path);
        WinSvcUtil.createService(serviceName, path);
    }

    @Override
    public void startService(String serviceName) {
        log.info("startService: {}", serviceName);
        WinSvcUtil.startService(serviceName);
    }

    @Override
    public void stopService(String serviceName) {
        log.info("stopService {}", serviceName);
        WinSvcUtil.stopService(serviceName);
    }

    @Override
    public void deleteService(String serviceName) {
        log.info("deleteService {}", serviceName);
        WinSvcUtil.deleteService(serviceName);
    }

    @Override
    public int queryServiceStatus(String serviceName) {
        return WinSvcUtil.queryServiceStatus(serviceName);
    }
}
