package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.RelayServer;
import io.jaspercloud.sdwan.tranport.RelayServerConfig;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class RelayServerBean implements InitializingBean, DisposableBean {

    private RelayServerConfig config;
    private RelayServer relayServer;

    public RelayServerBean(RelayServerConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        relayServer = new RelayServer(config, () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
    }

    @Override
    public void destroy() throws Exception {
        relayServer.stop();
    }
}