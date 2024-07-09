package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.RelayServer;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class SdWanControllerServer implements InitializingBean, DisposableBean {

    private SdWanServerConfig config;
    private SdWanServer sdWanServer;
    private RelayServer relayServer;

    public SdWanControllerServer(SdWanServerConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanServer = new SdWanServer(config, () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
            }
        });
        sdWanServer.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        sdWanServer.destroy();
    }
}
