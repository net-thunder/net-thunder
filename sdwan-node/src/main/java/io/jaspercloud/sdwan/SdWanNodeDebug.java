package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.tranport.P2pClient;
import io.jaspercloud.sdwan.tranport.RelayClient;

import java.util.concurrent.CountDownLatch;

public class SdWanNodeDebug {

    public static void main(String[] args) throws Exception {
        Logger logger = new LoggerSystem().initUserDir();
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        String controllerServer = config.getControllerServer();
        System.out.println("controllerServer: " + controllerServer);
        String stunServer = config.getStunServer();
        System.out.println("stunServer: " + stunServer);
        String relayServer = config.getRelayServer();
        System.out.println("relayServer: " + relayServer);
        String vipCidr = tunSdWanNode.getVipCidr();
        System.out.println("vipCidr: " + vipCidr);
        int sdwanLocalPort = tunSdWanNode.getSdWanClient().getLocalPort();
        System.out.println("sdwanLocalPort: " + sdwanLocalPort);
        P2pClient p2pClient = tunSdWanNode.getIceClient().getP2pClient();
        int p2pLocalPort = p2pClient.getLocalPort();
        System.out.println("p2pLocalPort: " + p2pLocalPort);
        RelayClient relayClient = tunSdWanNode.getIceClient().getRelayClient();
        String curToken = relayClient.getCurToken();
        System.out.println("curToken: " + curToken);
        int relayLocalPort = relayClient.getLocalPort();
        System.out.println("relayLocalPort: " + relayLocalPort);
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
