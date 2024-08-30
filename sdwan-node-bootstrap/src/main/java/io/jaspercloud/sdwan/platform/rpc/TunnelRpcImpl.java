package io.jaspercloud.sdwan.platform.rpc;

import io.jaspercloud.sdwan.node.TunSdWanNode;

public class TunnelRpcImpl implements TunnelRpc {

    private TunSdWanNode tunSdWanNode;

    public TunnelRpcImpl(TunSdWanNode tunSdWanNode) {
        this.tunSdWanNode = tunSdWanNode;
    }

    @Override
    public TunnelInfo getTunnelInfo() {
        TunnelInfo tunnelInfo = new TunnelInfo();
        String controllerServer = tunSdWanNode.getConfig().getControllerServer();
        tunnelInfo.setControllerServer(controllerServer);
        String stunServer = tunSdWanNode.getConfig().getStunServer();
        tunnelInfo.setStunServer(stunServer);
        String relayServer = tunSdWanNode.getConfig().getRelayServer();
        tunnelInfo.setRelayServer(relayServer);
        String vipCidr = tunSdWanNode.getVipCidr();
        tunnelInfo.setVipCidr(vipCidr);
        String localVip = tunSdWanNode.getLocalVip();
        tunnelInfo.setLocalVip(localVip);
        int sdwanLocalPort = tunSdWanNode.getSdWanClient().getLocalPort();
        tunnelInfo.setSdwanLocalPort(sdwanLocalPort);
        int p2pLocalPort = tunSdWanNode.getIceClient().getP2pClient().getLocalPort();
        tunnelInfo.setP2pLocalPort(p2pLocalPort);
        int relayLocalPort = tunSdWanNode.getIceClient().getRelayClient().getLocalPort();
        tunnelInfo.setRelayLocalPort(relayLocalPort);
        return tunnelInfo;
    }
}
