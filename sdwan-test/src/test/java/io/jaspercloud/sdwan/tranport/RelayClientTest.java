package io.jaspercloud.sdwan.tranport;

import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class RelayClientTest {

    @Test
    public void test() throws Exception {
        RelayServerConfig config = RelayServerConfig.builder()
                .bindPort(1300)
                .heartTimeout(15 * 1000)
                .build();
        RelayServer relayServer = new RelayServer(config, () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
        RelayClient relayClient = new RelayClient(new RelayClient.Config(), () -> new ChannelInboundHandlerAdapter());
        relayClient.start();
        String token = relayClient.regist(new InetSocketAddress("127.0.0.1", 1300), 3000).get();
        System.out.println("token: " + token);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
