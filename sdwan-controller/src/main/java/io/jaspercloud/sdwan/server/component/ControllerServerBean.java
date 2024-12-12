package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.ControllerServer;
import io.jaspercloud.sdwan.tranport.ControllerServerConfig;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ControllerServerBean implements FactoryBean<ControllerServer>, InitializingBean, DisposableBean {

    private ControllerServerConfig config;
    private SdWanDataService dataService;
    private ControllerServer controllerServer;

    public ControllerServerBean(ControllerServerConfig config, SdWanDataService dataService) {
        this.config = config;
        this.dataService = dataService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        controllerServer.start();
    }

    @Override
    public void destroy() throws Exception {
        controllerServer.stop();
    }

    @Override
    public ControllerServer getObject() throws Exception {
        return controllerServer;
    }

    @Override
    public Class<?> getObjectType() {
        return ControllerServer.class;
    }
}