package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class SdWanServerBean implements FactoryBean<SdWanServer>, InitializingBean, DisposableBean {

    private SdWanServerConfig config;
    private SdWanDataService dataService;
    private SdWanServer sdWanServer;

    public SdWanServerBean(SdWanServerConfig config, SdWanDataService dataService) {
        this.config = config;
        this.dataService = dataService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
    }

    @Override
    public void destroy() throws Exception {
        sdWanServer.stop();
    }

    @Override
    public SdWanServer getObject() throws Exception {
        return sdWanServer;
    }

    @Override
    public Class<?> getObjectType() {
        return SdWanServer.class;
    }
}