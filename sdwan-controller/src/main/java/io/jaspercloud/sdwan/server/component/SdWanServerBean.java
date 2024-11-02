package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class SdWanServerBean implements InitializingBean, DisposableBean {

    private SdWanServerConfig config;
    private SdWanServer sdWanServer;

    public SdWanServerBean(SdWanServerConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanServer = new SdWanServer(config, () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
    }

    @Override
    public void destroy() throws Exception {
        sdWanServer.stop();
    }
}