package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferSignallingTest {

    @Test
    public void test() throws Exception {
        String localAddress = InetAddress.getLocalHost().getHostAddress();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1001)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build());
        sdWanNode1.start();
        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1002)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build());
        sdWanNode2.start();
        new Thread(() -> {
            while (true) {
                SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP("10.5.0.13")
                        .setDstIP("10.5.0.14")
                        .setPayload(ByteString.copyFrom("test1".getBytes()))
                        .build();
                sdWanNode1.sendIpPacket(ipPacket);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP("10.5.0.14")
                        .setDstIP("10.5.0.13")
                        .setPayload(ByteString.copyFrom("test2".getBytes()))
                        .build();
                sdWanNode2.sendIpPacket(ipPacket);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
