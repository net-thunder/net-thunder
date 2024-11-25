package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferSignallingTest {

    @Test
    public void test() throws Exception {
        String localAddress = InetAddress.getLocalHost().getHostAddress();
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setLocalAddress(localAddress);
        nodeConfig1.setP2pPort(1001);
        TestSdWanNode sdWanNode1 = new TestSdWanNode(nodeConfig1);
        sdWanNode1.start();
        SdWanNodeConfig nodeConfig2 = new SdWanNodeConfig();
        nodeConfig2.setControllerServer("127.0.0.1:1800");
        nodeConfig2.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig2.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig2.setLocalAddress(localAddress);
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2);
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
