package io.jaspercloud.sdwan.server.component;

import io.jaspercloud.sdwan.tranport.StunServer;
import io.jaspercloud.sdwan.tranport.StunServerConfig;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class StunServerBean implements InitializingBean, DisposableBean {

    private StunServerConfig config;
    private StunServer stunServer;

    public StunServerBean(StunServerConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        stunServer = new StunServer(config, () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
    }

    @Override
    public void destroy() throws Exception {
        stunServer.stop();
    }
}
