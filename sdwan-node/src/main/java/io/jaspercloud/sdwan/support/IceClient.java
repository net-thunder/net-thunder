package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.tranport.P2pClient;
import io.jaspercloud.sdwan.tranport.RelayClient;
import io.jaspercloud.sdwan.tranport.TransportLifecycle;
import io.jaspercloud.sdwan.util.AddressType;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Slf4j
public class IceClient implements TransportLifecycle {

    private SdWanNodeConfig config;
    private BaseSdWanNode sdWanNode;
    private Supplier<ChannelHandler> handler;

    private P2pClient p2pClient;
    private RelayClient relayClient;
    private P2pTransportManager p2pTransportManager;

    public P2pClient getP2pClient() {
        return p2pClient;
    }

    public P2pTransportManager getP2pTransportManager() {
        return p2pTransportManager;
    }

    public IceClient(SdWanNodeConfig config, BaseSdWanNode sdWanNode, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.sdWanNode = sdWanNode;
        this.handler = handler;
    }

    public NatAddress parseNatAddress(int timeout) throws Exception {
        return p2pClient.parseNatAddress(timeout);
    }

    public String registRelay(int timeout) throws Exception {
        return relayClient.regist(timeout).get();
    }

    public CompletableFuture<InetSocketAddress> ping(String vip, InetSocketAddress address, long timeout) {
        return p2pClient.parseNATAddress(address, timeout)
                .thenApply(parseAddress -> {
                    p2pTransportManager.addP2pAddress(vip, parseAddress);
                    return parseAddress;
                });
    }

    public void sendNode(SDWanProtos.NodeInfo nodeInfo, byte[] bytes) {
        Set<InetSocketAddress> p2pAddressSet = p2pTransportManager.getP2pAddressSet(nodeInfo.getVip());
        if (!p2pAddressSet.isEmpty()) {
            InetSocketAddress p2pAddress = selectAddress(p2pAddressSet);
            p2pClient.transfer(p2pAddress, bytes);
            return;
        }
        List<UriComponents> uriList = nodeInfo.getAddressUriList()
                .stream()
                .map(u -> UriComponentsBuilder.fromUriString(u).build())
                .collect(Collectors.toList());
        //relay
        UriComponents relay = uriList.stream().filter(e -> AddressType.RELAY.equals(e.getScheme())).findAny().get();
        String token = relay.getQueryParams().getFirst("token");
        relayClient.transfer(token, bytes);
        //p2p
        List<UriComponents> p2pList = uriList.stream().filter(e -> !AddressType.RELAY.equals(e.getScheme())).collect(Collectors.toList());
        p2pList.forEach(uri -> {
            InetSocketAddress pingAddr = new InetSocketAddress(uri.getHost(), uri.getPort());
            log.debug("ping: {}", pingAddr);
            ping(nodeInfo.getVip(), pingAddr, 3000)
                    .thenAccept(address -> {
                        log.debug("ping success: {}", uri.toString());
                    });
            SDWanProtos.P2pOffer p2pOfferReq = SDWanProtos.P2pOffer.newBuilder()
                    .setSrcVIP(sdWanNode.getLocalVip())
                    .setDstVIP(nodeInfo.getVip())
                    .setOfferAddress(SDWanProtos.SocketAddress.newBuilder()
                            .setIp(sdWanNode.getMappingAddress().getMappingAddress().getHostString())
                            .setPort(p2pClient.getLocalPort())
                            .build())
                    .setAnswerAddress(SDWanProtos.SocketAddress.newBuilder()
                            .setIp(uri.getHost())
                            .setPort(uri.getPort())
                            .build())
                    .build();
            sdWanNode.getSdWanClient().offer(p2pOfferReq, 3000)
                    .whenComplete((resp, ex) -> {
                        SDWanProtos.MessageCode code = resp.getCode();
                        if (SDWanProtos.MessageCode.Success.equals(code)) {
                            Set<InetSocketAddress> addrSet = p2pTransportManager.getP2pAddressSet(nodeInfo.getVip());
                            InetSocketAddress p2pAddress = selectAddress(addrSet);
                            p2pClient.transfer(p2pAddress, bytes);
                        } else {
                            relayClient.transfer(token, bytes);
                        }
                    });
        });
    }

    private InetSocketAddress selectAddress(Set<InetSocketAddress> addressSet) {
        List<InetSocketAddress> list = new ArrayList<>(addressSet);
        if (list.isEmpty()) {
            return null;
        }
        int rand = RandomUtils.nextInt(0, list.size());
        InetSocketAddress address = list.get(rand);
        return address;
    }

    @Override
    public boolean isRunning() {
        return relayClient.isRunning();
    }

    @Override
    public void start() throws Exception {
        p2pClient = new P2pClient(config.getStunServer(), config.getP2pPort(), config.getP2pHeartTime(), handler);
        relayClient = new RelayClient(config.getRelayServer(), config.getP2pHeartTime(), handler);
        p2pTransportManager = new P2pTransportManager(p2pClient, config.getP2pHeartTime());
        p2pTransportManager.afterPropertiesSet();
        p2pClient.start();
        relayClient.start();
        log.info("ice client started");
    }

    @Override
    public void stop() throws Exception {
        if (null != p2pClient) {
            p2pClient.stop();
        }
        if (null != relayClient) {
            relayClient.stop();
        }
        if (null != p2pTransportManager) {
            p2pTransportManager.destroy();
        }
    }
}
